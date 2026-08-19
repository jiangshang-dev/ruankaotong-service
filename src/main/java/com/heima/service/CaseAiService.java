package com.heima.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.config.AiProperties;
import com.heima.dto.CaseAiDtos.CaseExplainRequest;
import com.heima.dto.CaseAiDtos.CaseImage;
import com.heima.dto.CaseAiDtos.CaseQuestionAnswer;
import com.heima.dto.CaseAiDtos.CaseScoreRequest;
import com.heima.dto.CaseAiDtos.CaseScoreResponse;
import com.heima.dto.CaseAiDtos.CaseSolveRequest;
import com.heima.dto.CaseAiDtos.CaseSolveResponse;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.dto.EssayAiDtos.ScoreDimension;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 软考案例分析 AI：多模态识图解答 / 评分。每次新建 ReActAgent，无会话记忆。
 */
@Slf4j
@Service
public class CaseAiService {

    private static final int MAX_IMAGES = 8;

    private final AiProperties props;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final EssayGuideHistoryService historyService;
    private final AgentStateStore agentStateStore;
    private final Path harnessWorkspace;

    public CaseAiService(
            AiProperties props,
            PromptLoader promptLoader,
            ObjectMapper objectMapper,
            EssayGuideHistoryService historyService,
            AgentStateStore agentStateStore) {
        this.props = props;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.historyService = historyService;
        this.agentStateStore = agentStateStore;
        this.harnessWorkspace = Path.of(System.getProperty("java.io.tmpdir"), "ruankao-harness");
        try {
            Files.createDirectories(this.harnessWorkspace);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 HarnessAgent 工作目录: " + this.harnessWorkspace, e);
        }
    }

    public CaseSolveResponse solve(CaseSolveRequest req) {
        List<CaseImage> images = normalizeImages(req == null ? null : req.images());
        if (images.isEmpty() && (req == null || !StringUtils.hasText(req.topicText()))) {
            throw new IllegalArgumentException("请先粘贴题目截图后再解答");
        }
        String sys = promptLoader.load(props.getCaseSolvePrompt());
        log.info("系统提示词（ai案例分析解答）:{}", sys);
        String user = buildSolveUserPrompt(req);
        log.info("用户提问（ai案例分析解答）:{}", user);
        String raw = callVision("ruankao-case-solve", sys, user, images);
        return parseSolve(raw);
    }

    public CaseScoreResponse score(CaseScoreRequest req) {
        if (req == null || !StringUtils.hasText(req.answerText())) {
            throw new IllegalArgumentException("请先按题号填写答案后再评分");
        }
        List<CaseImage> images = normalizeImages(req.images());
        if (images.isEmpty() && !StringUtils.hasText(req.topicText())) {
            throw new IllegalArgumentException("请先粘贴题目截图后再评分");
        }
        String sys = promptLoader.load(props.getCaseScorePrompt());
        log.info("系统提示词：{}", sys);
        String user = buildScoreUserPrompt(req);
        log.info("用户问题：{}", user);
        String raw = callVision("ruankao-case-score", sys, user, images);
        return parseScore(raw);
    }

    public EssayGuideHistoryResponse listExplainHistory(String subjectId, String fileName) {
        return historyService.list(subjectId, fileName, "case");
    }

    /**
     * 案例分析讲解流式输出：HarnessAgent.streamEvents，接口直接返回 Flux。
     */
    public Flux<EssayGuideStreamEvent> explainStream(CaseExplainRequest req) {
        List<CaseImage> images = normalizeImages(req == null ? null : req.images());
        if (images.isEmpty() && (req == null || !StringUtils.hasText(req.topicText()))) {
            throw new IllegalArgumentException("请先粘贴题目截图后再讲解");
        }

        String sys = promptLoader.load(props.getCaseExplainStreamPrompt());
        String user = buildExplainUserPrompt(req);
        Msg userMsg = buildUserMsg(user, images);

        String recordId = UUID.randomUUID().toString().replace("-", "");
        long createdAt = System.currentTimeMillis();
        String subjectId = EssayGuideHistoryService.userId(req == null ? null : req.subjectId());
        String fileName = EssayGuideHistoryService.sessionId(req == null ? null : req.fileName(), "case");
        StringBuilder acc = new StringBuilder();
        StringBuilder preview = new StringBuilder();

        HarnessAgent agent = buildExplainHarness(
                sys, images.isEmpty() ? props.getModelName() : props.resolveVisionModelName());
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
                        sink.next(new EssayGuideStreamEvent(
                                "delta", delta, acc.toString(), preview.toString(), recordId, createdAt));
                        return;
                    }
                    if (event instanceof AgentResultEvent resultEvent && acc.isEmpty()) {
                        String text = resultEvent.getResult() != null
                                ? resultEvent.getResult().getTextContent()
                                : null;
                        if (StringUtils.hasText(text)) {
                            acc.append(text);
                            sink.next(new EssayGuideStreamEvent(
                                    "delta", text, acc.toString(), preview.toString(), recordId, createdAt));
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
                    log.error("案例分析讲解流式失败", e);
                    String markdown = acc.toString().trim();
                    String thinking = preview.toString().trim();
                    String msg = e.getMessage() == null ? "讲解生成失败" : e.getMessage();
                    return Flux.just(new EssayGuideStreamEvent("error", msg, markdown, thinking, recordId, createdAt));
                });
    }

    private HarnessAgent buildExplainHarness(String sysPrompt, String modelName) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(modelName)
                .stream(true)
                .build();

        return HarnessAgent.builder()
                .name("ruankao-case-explain")
                .sysPrompt(sysPrompt)
                .model(model)
                .workspace(harnessWorkspace)
                .stateStore(agentStateStore)
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

    private static String buildExplainUserPrompt(CaseExplainRequest req) {
        return """
                科目：%s
                标题：%s

                题目区附加文字（截图之外，可空）：
                %s

                考生当前作答（可空，仅作对照，不要当成必须润色的对象）：
                %s

                上面同时附上了题目截图。请识读全部图片中的试题，按系统要求用 Markdown 流式输出解题技巧讲解与参考答案。
                """.formatted(
                nullToEmpty(req == null ? null : req.subject()),
                nullToEmpty(req == null ? null : req.title()),
                nullToEmpty(req == null ? null : req.topicText()),
                nullToEmpty(req == null ? null : req.answerText()));
    }

    private String callVision(String agentName, String sysPrompt, String userText, List<CaseImage> images) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(props.resolveVisionModelName())
                .stream(props.isStream())
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name(agentName)
                .sysPrompt(sysPrompt)
                .model(model)
                .maxIters(1)
                .build();

        Msg msg = buildUserMsg(userText, images);
        Msg response = agent.call(msg).block(Duration.ofSeconds(Math.max(30, props.getTimeoutSeconds())));
        if (response == null || !StringUtils.hasText(response.getTextContent())) {
            throw new IllegalStateException("模型未返回有效内容");
        }
        return response.getTextContent().trim();
    }

    private static Msg buildUserMsg(String userText, List<CaseImage> images) {
        List<ContentBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock.builder().text(userText).build());
        for (CaseImage image : images) {
            String b64 = stripDataUrl(image.base64());
            if (!StringUtils.hasText(b64)) {
                continue;
            }
            String mime = StringUtils.hasText(image.mimeType()) ? image.mimeType().trim() : "image/png";
            blocks.add(ImageBlock.builder()
                    .source(Base64Source.builder()
                            .data(b64)
                            .mediaType(mime)
                            .build())
                    .build());
        }
        return Msg.builder()
                .role(MsgRole.USER)
                .content(blocks)
                .build();
    }

    private static String buildSolveUserPrompt(CaseSolveRequest req) {
        return """
                科目：%s
                标题：%s

                题目区附加文字（截图之外，可空）：
                %s

                上面同时附上了题目截图。请识读全部图片中的试题，按系统要求输出 JSON 参考答案。
                """.formatted(
                nullToEmpty(req == null ? null : req.subject()),
                nullToEmpty(req == null ? null : req.title()),
                nullToEmpty(req == null ? null : req.topicText()));
    }

    private static String buildScoreUserPrompt(CaseScoreRequest req) {
        return """
                科目：%s
                标题：%s

                题目区附加文字（截图之外，可空）：
                %s

                考生作答（只需标清题号，请自行对齐到各问）：
                %s

                上面同时附上了题目截图。请对照试题与作答，按系统要求输出 JSON 评分结果。
                """.formatted(
                nullToEmpty(req.subject()),
                nullToEmpty(req.title()),
                nullToEmpty(req.topicText()),
                nullToEmpty(req.answerText()));
    }

    private CaseSolveResponse parseSolve(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<CaseQuestionAnswer> questions = new ArrayList<>();
            JsonNode arr = root.path("questions");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    questions.add(new CaseQuestionAnswer(
                            n.path("questionNo").asText(""),
                            n.path("stem").asText(""),
                            n.path("answer").asText("")));
                }
            }
            String answerText = root.path("answerText").asText("");
            if (!StringUtils.hasText(answerText) && !questions.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (CaseQuestionAnswer q : questions) {
                    if (!sb.isEmpty()) {
                        sb.append("\n\n");
                    }
                    sb.append("【").append(StringUtils.hasText(q.questionNo()) ? q.questionNo() : "问题")
                            .append("】\n")
                            .append(nullToEmpty(q.answer()));
                }
                answerText = sb.toString();
            }
            return new CaseSolveResponse(
                    root.path("title").asText(""),
                    answerText,
                    questions,
                    raw);
        } catch (Exception e) {
            return new CaseSolveResponse("", raw, List.of(), raw);
        }
    }

    private CaseScoreResponse parseScore(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ScoreDimension> dims = new ArrayList<>();
            JsonNode arr = root.path("dimensions");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    dims.add(new ScoreDimension(
                            n.path("name").asText(""),
                            n.path("score").asInt(0),
                            n.path("max").asInt(0),
                            n.path("comment").asText("")));
                }
            }
            int total = root.path("totalScore").asInt(0);
            if (!dims.isEmpty()) {
                int sum = dims.stream().mapToInt(ScoreDimension::score).sum();
                if (sum > 0) {
                    total = sum;
                }
            }
            total = Math.max(0, Math.min(75, total));
            return new CaseScoreResponse(
                    total,
                    total >= 45 ? "合格" : "不及格",
                    dims,
                    root.path("summary").asText(""),
                    readStringList(root.path("strengths")),
                    readStringList(root.path("improvements")),
                    raw);
        } catch (Exception e) {
            return new CaseScoreResponse(
                    0,
                    "不及格",
                    List.of(),
                    "模型返回内容无法解析为标准 JSON，请查看 raw。",
                    List.of(),
                    List.of(),
                    raw);
        }
    }

    private static List<CaseImage> normalizeImages(List<CaseImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<CaseImage> out = new ArrayList<>();
        for (CaseImage image : images) {
            if (image == null || !StringUtils.hasText(image.base64())) {
                continue;
            }
            out.add(image);
            if (out.size() >= MAX_IMAGES) {
                break;
            }
        }
        return out;
    }

    private static String stripDataUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.trim();
        int comma = text.indexOf(',');
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:") && comma > 0) {
            return text.substring(comma + 1).trim();
        }
        return text;
    }

    private static List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                if (n.isTextual()) {
                    list.add(n.asText());
                }
            }
        }
        return list;
    }

    private static String extractJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
