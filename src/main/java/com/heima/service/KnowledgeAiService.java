package com.heima.service;

import com.heima.config.AiProperties;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.dto.KnowledgeAiDtos.KnowledgeTutorRequest;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import io.agentscope.harness.agent.memory.MemoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 综合知识 AI 辅导：HarnessAgent 流式 + Redis agent_state 会话。
 */
@Slf4j
@Service
public class KnowledgeAiService {

    private static final String SESSION_PREFIX = "knowledge";
    private static final int MAX_NOTE_CHARS = 12_000;
    private static final int MAX_FOLLOWUP_NOTE_CHARS = 2_000;

    private final AiProperties props;
    private final PromptLoader promptLoader;
    private final EssayGuideHistoryService historyService;
    private final AgentStateStore agentStateStore;
    private final KnowledgeDocCatalog knowledgeDocCatalog;
    /**
     * AgentScope Harness 运行时沙箱，不是讲义目录。
     * 用系统临时目录，换电脑/服务器都能写；讲义请走 classpath:docs。
     */
    private final Path harnessWorkspace;

    public KnowledgeAiService(
            AiProperties props,
            PromptLoader promptLoader,
            EssayGuideHistoryService historyService,
            AgentStateStore agentStateStore,
            KnowledgeDocCatalog knowledgeDocCatalog) {
        this.props = props;
        this.promptLoader = promptLoader;
        this.historyService = historyService;
        this.agentStateStore = agentStateStore;
        this.knowledgeDocCatalog = knowledgeDocCatalog;
        this.harnessWorkspace = Path.of(System.getProperty("java.io.tmpdir"), "ruankao-harness");
        try {
            Files.createDirectories(this.harnessWorkspace);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 HarnessAgent 工作目录: " + this.harnessWorkspace, e);
        }
    }

    public EssayGuideHistoryResponse listTutorHistory(String subjectId, String fileName, String email) {
        return historyService.list(subjectId, fileName, SESSION_PREFIX, true, email);
    }

    /**
     * 综合知识辅导流式输出：HarnessAgent.streamEvents，接口直接返回 Flux。
     */
    public Flux<EssayGuideStreamEvent> tutorStream(KnowledgeTutorRequest req, String email) {
        if (req == null || (!StringUtils.hasText(req.title())
                && !StringUtils.hasText(req.noteText()) && !StringUtils.hasText(req.question()))) {
            throw new IllegalArgumentException("请先填写笔记标题、正文或要问的问题");
        }

        String sys = promptLoader.load(props.getKnowledgeTutorStreamPrompt());
        String user = buildTutorUserPrompt(req);
        Msg userMsg = Msg.builder().role(MsgRole.USER).textContent(user).build();

        String recordId = UUID.randomUUID().toString().replace("-", "");
        long createdAt = System.currentTimeMillis();
        String subjectId = EssayGuideHistoryService.userId(req.subjectId(), email);
        String fileName = EssayGuideHistoryService.sessionId(req.fileName(), SESSION_PREFIX);
        StringBuilder acc = new StringBuilder();
        StringBuilder preview = new StringBuilder();

        HarnessAgent agent = buildTutorHarness(sys);
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .sessionId(fileName)
                .userId(subjectId)
                .build();

        Flux<AgentEvent> events = agent.streamEvents(userMsg, runtimeContext);
        return events.subscribeOn(Schedulers.boundedElastic())
                .<EssayGuideStreamEvent>handle((event, sink) -> {
                    if (event instanceof ThinkingBlockDeltaEvent thinkingEvent) {
                        String delta = thinkingEvent.getDelta();
                        if (!StringUtils.hasText(delta)) {
                            return;
                        }
                        preview.append(delta);
                        sink.next(new EssayGuideStreamEvent(
                                "think_delta", delta, acc.toString(), preview.toString(), recordId, createdAt));
                        return;
                    }
                    if (event instanceof TextBlockDeltaEvent textEvent) {
                        String delta = textEvent.getDelta();
                        if (!StringUtils.hasText(delta)) {
                            return;
                        }
                        acc.append(delta);
                        sink.next(new EssayGuideStreamEvent("delta", delta, acc.toString(), preview.toString(), recordId, createdAt));
                        return;
                    }
                    if (event instanceof AgentResultEvent resultEvent && acc.isEmpty()) {
                        String text = resultEvent.getResult() != null ? resultEvent.getResult().getTextContent() : null;
                        if (StringUtils.hasText(text)) {
                            acc.append(text);
                            sink.next(new EssayGuideStreamEvent("delta", text, acc.toString(), preview.toString(), recordId, createdAt));
                        }
                    }
                })
                .concatWith(Mono.fromSupplier(() -> {
                    String markdown = acc.toString().trim();
                    String thinking = preview.toString().trim();
                    return new EssayGuideStreamEvent("done", "", markdown, thinking, recordId, createdAt);
                }))
                .timeout(Duration.ofSeconds(Math.max(30, props.getTimeoutSeconds())))
                .onErrorResume(e -> {
                    log.error("综合知识辅导流式失败", e);
                    String markdown = acc.toString().trim();
                    String thinking = preview.toString().trim();
                    String msg = e.getMessage() == null ? "辅导生成失败" : e.getMessage();
                    return Flux.just(new EssayGuideStreamEvent("error", msg, markdown, thinking, recordId, createdAt));
                });
    }

    private HarnessAgent buildTutorHarness(String sysPrompt) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(props.getModelName())
                .stream(true)
                .build();

        MemoryConfig.FlushTrigger flushTrigger = MemoryConfig.FlushTrigger.throttled(Duration.ofMinutes(10));
        MemoryConfig memoryConfig = MemoryConfig.builder()
                .flushTrigger(flushTrigger)
                .consolidationMinGap(Duration.ofMinutes(50))
                .build();

        return HarnessAgent.builder()
                .name("ruankao-knowledge-tutor")
                .sysPrompt(sysPrompt)
                .model(model)
                .workspace(harnessWorkspace)
                .stateStore(agentStateStore)
                .memory(memoryConfig)
                .maxIters(1)
                .generateOptions(GenerateOptions.builder().build())
                .toolkit(new Toolkit())
                .disableFilesystemTools()
                .disableShellTool()
                .disableWorkspaceContext()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableSubagents()
                .disableDynamicSubagents()
                .disableToolsConfig()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .disableCompaction()
                .disableToolResultEviction()
                .disableAtPathExpansion()
                .skillsEnabled(false)
                .build();
    }

    private String buildTutorUserPrompt(KnowledgeTutorRequest req) {
        String docs = knowledgeDocCatalog.promptBlock(req.title(), req.question());
        boolean followUp = Boolean.TRUE.equals(req.followUp()) && StringUtils.hasText(req.question());
        if (followUp) {
            StringBuilder sb = new StringBuilder();
            sb.append("【连续追问】结合此前本会话讲解继续回答，不要无故重写整篇辅导。\n\n");
            sb.append(docs).append("\n\n");
            sb.append("当前笔记标题（可能已改，仅参考）：\n");
            sb.append(emptyAsDash(req.title())).append("\n\n");
            sb.append("当前笔记正文摘录（可能已改，仅参考）：\n");
            sb.append(clip(req.noteText(), MAX_FOLLOWUP_NOTE_CHARS)).append("\n\n");
            sb.append("追问：\n");
            sb.append(req.question().trim());
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("科目：").append(emptyAsDash(req.subject())).append('\n');
        sb.append("笔记标题：").append(emptyAsDash(req.title())).append("\n\n");
        sb.append(docs).append("\n\n");
        sb.append("笔记正文（考生当前知识点笔记，可空）：\n");
        sb.append(clip(req.noteText(), MAX_NOTE_CHARS)).append("\n\n");
        if (StringUtils.hasText(req.question())) {
            sb.append("考生本次要求：\n");
            sb.append(req.question().trim()).append("\n\n");
        } else {
            sb.append("考生本次要求：请针对当前笔记主题做综合知识系统辅导。\n\n");
        }
        sb.append("请按系统要求用 Markdown 流式输出综合知识辅导。");
        return sb.toString();
    }

    private static String clip(String text, int maxChars) {
        String value = text == null ? "" : text.trim();
        if (value.length() <= maxChars) {
            return value.isEmpty() ? "（空）" : value;
        }
        return value.substring(0, maxChars) + "\n…（已截断）";
    }

    private static String emptyAsDash(String s) {
        return StringUtils.hasText(s) ? s.trim() : "（未填）";
    }

    public void stop() {

    }
}
