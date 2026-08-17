package com.heima.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.config.AiProperties;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideRequest;
import com.heima.dto.EssayAiDtos.EssayGuideResponse;
import com.heima.dto.EssayAiDtos.EssayGuideSection;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.dto.EssayAiDtos.EssayImage;
import com.heima.dto.EssayAiDtos.EssayPolishRequest;
import com.heima.dto.EssayAiDtos.EssayPolishResponse;
import com.heima.dto.EssayAiDtos.EssayProjectExample;
import com.heima.dto.EssayAiDtos.EssayScoreRequest;
import com.heima.dto.EssayAiDtos.EssayScoreResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 软考论文 AI。论文指导流式会话由 AgentScope RedisAgentStateStore 按 (userId, sessionId) 落 agent_state。
 */
@Slf4j
@Service
public class EssayAiService {

    private static final Pattern ABSTRACT_BLOCK =
            Pattern.compile("【摘要】\\s*([\\s\\S]*?)(?=【正文】|$)");
    private static final Pattern BODY_BLOCK =
            Pattern.compile("【正文】\\s*([\\s\\S]*)$");

    private final AiProperties props;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final EssayGuideHistoryService historyService;
    private final AgentStateStore agentStateStore;
    private final Path harnessWorkspace;

    public EssayAiService(AiProperties props, PromptLoader promptLoader,
            ObjectMapper objectMapper, EssayGuideHistoryService historyService, AgentStateStore agentStateStore) {
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

    public EssayPolishResponse polish(EssayPolishRequest req) {
        String part = normalizePart(req.part());
        String sys = promptLoader.load(props.getPolishPrompt());
        String user = buildPolishUserPrompt(req, part);

        String raw = callOnce("ruankao-polish", sys, user);
        return parsePolish(part, raw, req);
    }

    public EssayScoreResponse score(EssayScoreRequest req) {
        if (!StringUtils.hasText(req.abstractText()) && !StringUtils.hasText(req.bodyText())) {
            throw new IllegalArgumentException("请先填写摘要或正文后再评分");
        }
        String sys = promptLoader.load(props.getScorePrompt());
        String user = buildScoreUserPrompt(req);
        String raw = callOnce("ruankao-score", sys, user);
        return parseScore(raw);
    }

    public EssayGuideResponse guide(EssayGuideRequest req) {
        List<EssayImage> images = normalizeImages(req == null ? null : req.images());
        if (images.isEmpty() && (req == null || !StringUtils.hasText(req.topic()))) {
            throw new IllegalArgumentException("请先填写或粘贴论文题目后再指导");
        }
        String sys = promptLoader.load(props.getGuidePrompt());
        String user = buildGuideUserPrompt(req);
        String raw = callVision("ruankao-essay-guide", sys, user, images);
        return parseGuide(raw);
    }

    public EssayGuideHistoryResponse listGuideHistory(String subjectId, String fileName) {
        return historyService.list(subjectId, fileName);
    }

    /**
     * 论文指导流式输出：HarnessAgent.streamEvents，接口直接返回 Flux。
     */
    public Flux<EssayGuideStreamEvent> guideStream(EssayGuideRequest req) {
        List<EssayImage> images = normalizeImages(req == null ? null : req.images());
        if (images.isEmpty() && (req == null || !StringUtils.hasText(req.topic()))) {
            throw new IllegalArgumentException("请先填写或粘贴论文题目后再指导");
        }

        String sys = promptLoader.load(props.getGuideStreamPrompt());
        String user = buildGuideStreamUserPrompt(req);
        Msg userMsg = buildUserMsg(user, images);

        String recordId = UUID.randomUUID().toString().replace("-", "");
        long createdAt = System.currentTimeMillis();
        String subjectId = EssayGuideHistoryService.userId(req == null ? null : req.subjectId());
        String fileName = EssayGuideHistoryService.sessionId(req == null ? null : req.fileName());
        StringBuilder acc = new StringBuilder();
        StringBuilder preview = new StringBuilder();

        HarnessAgent agent = buildGuideHarness(sys, images.isEmpty() ? props.getModelName() : props.resolveVisionModelName());
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .sessionId(fileName)
                .userId(subjectId)
                .build();

        Flux<AgentEvent> agentEvent = agent.streamEvents(userMsg, runtimeContext);
        return agentEvent.subscribeOn(Schedulers.boundedElastic())
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
                    log.error("论文指导流式失败", e);
                    String markdown = acc.toString().trim();
                    String thinking = preview.toString().trim();
                    String msg = e.getMessage() == null ? "指导生成失败" : e.getMessage();
                    return Flux.just(new EssayGuideStreamEvent("error", msg, markdown, thinking, recordId, createdAt));
                });
    }

    private HarnessAgent buildGuideHarness(String sysPrompt, String modelName) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(modelName)
                .stream(true)
                .build();

        return HarnessAgent.builder()
                .name("ruankao-essay-guide")
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

    private String callOnce(String agentName, String sysPrompt, String userText) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(props.getModelName())
                .stream(props.isStream())
                .build();

        // 无 toolkit、无 longTermMemory、无 session：单轮即弃
        ReActAgent agent = ReActAgent.builder()
                .name(agentName)
                .sysPrompt(sysPrompt)
                .model(model)
                .maxIters(1)
                .build();

        log.info("sys prompt:{}", agent.getSysPrompt());
        log.info("system prompt:{}", agent.getSystemPrompt());

        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userText)
                .build();

        Msg response = agent.call(msg).block(Duration.ofSeconds(Math.max(30, props.getTimeoutSeconds())));
        log.info("消息内容:{}", response.getTextContent());
        log.info("消息内容:{}", response.getContent());
        if (response == null || !StringUtils.hasText(response.getTextContent())) {
            throw new IllegalStateException("模型未返回有效内容");
        }
        return response.getTextContent().trim();
    }

    private String callVision(String agentName, String sysPrompt, String userText, List<EssayImage> images) {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(images.isEmpty() ? props.getModelName() : props.resolveVisionModelName())
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

    private static Msg buildUserMsg(String userText, List<EssayImage> images) {
        if (images == null || images.isEmpty()) {
            return Msg.builder().role(MsgRole.USER).textContent(userText).build();
        }
        List<ContentBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock.builder().text(userText).build());
        for (EssayImage image : images) {
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
        return Msg.builder().role(MsgRole.USER).content(blocks).build();
    }

    private static List<EssayImage> normalizeImages(List<EssayImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<EssayImage> out = new ArrayList<>();
        for (EssayImage image : images) {
            if (image == null || !StringUtils.hasText(image.base64())) {
                continue;
            }
            out.add(image);
            if (out.size() >= 8) {
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

    private static String normalizePart(String part) {
        if (!StringUtils.hasText(part)) {
            return "all";
        }
        String p = part.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "abstract", "摘要" -> "abstract";
            case "body", "正文" -> "body";
            default -> "all";
        };
    }

    private static String buildPolishUserPrompt(EssayPolishRequest req, String part) {
        StringBuilder sb = new StringBuilder();
        sb.append("科目：").append(nullToEmpty(req.subject())).append('\n');
        sb.append("题目与要求：\n").append(nullToEmpty(req.topic())).append("\n\n");
        sb.append("当前摘要：\n").append(nullToEmpty(req.abstractText())).append("\n\n");
        sb.append("当前正文：\n").append(nullToEmpty(req.bodyText())).append("\n\n");
        switch (part) {
            case "abstract" -> sb.append("请只润色【摘要】，只输出润色后的摘要纯文本。");
            case "body" -> sb.append("请只润色【正文】，只输出润色后的正文纯文本。");
            default -> sb.append("请润色摘要与正文，按【摘要】【正文】标记输出。");
        }
        return sb.toString();
    }

    private static String buildScoreUserPrompt(EssayScoreRequest req) {
        return """
                科目：%s

                题目与要求：
                %s

                摘要：
                %s

                正文：
                %s

                请按系统要求输出 JSON 评分结果。
                """.formatted(
                nullToEmpty(req.subject()),
                nullToEmpty(req.topic()),
                nullToEmpty(req.abstractText()),
                nullToEmpty(req.bodyText()));
    }

    private static String buildGuideUserPrompt(EssayGuideRequest req) {
        return """
                科目：%s

                论文题目与要求（文字，可空；若同时有截图，以截图为准并对照文字）：
                %s

                考生当前摘要（可空，仅作参考，不要当成必须润色的对象）：
                %s

                考生当前正文（可空）：
                %s

                请按系统要求输出 JSON 论文指导。
                """.formatted(
                nullToEmpty(req == null ? null : req.subject()),
                nullToEmpty(req == null ? null : req.topic()),
                nullToEmpty(req == null ? null : req.abstractText()),
                nullToEmpty(req == null ? null : req.bodyText()));
    }

    private static String buildGuideStreamUserPrompt(EssayGuideRequest req) {
        return """
                科目：%s

                论文题目与要求（文字，可空；若同时有截图，以截图为准并对照文字）：
                %s

                考生当前摘要（可空，仅作参考，不要当成必须润色的对象）：
                %s

                考生当前正文（可空）：
                %s

                请按系统要求用 Markdown 流式输出论文指导。
                """.formatted(
                nullToEmpty(req == null ? null : req.subject()),
                nullToEmpty(req == null ? null : req.topic()),
                nullToEmpty(req == null ? null : req.abstractText()),
                nullToEmpty(req == null ? null : req.bodyText()));
    }

    private EssayPolishResponse parsePolish(String part, String raw, EssayPolishRequest req) {
        if ("abstract".equals(part)) {
            return new EssayPolishResponse(part, stripMarkers(raw), req.bodyText(), raw);
        }
        if ("body".equals(part)) {
            return new EssayPolishResponse(part, req.abstractText(), stripMarkers(raw), raw);
        }
        String abs = extract(ABSTRACT_BLOCK, raw);
        String body = extract(BODY_BLOCK, raw);
        if (!StringUtils.hasText(abs) && !StringUtils.hasText(body)) {
            // 模型未按标记输出时，整段当作正文，摘要保持原样
            return new EssayPolishResponse(part, req.abstractText(), raw, raw);
        }
        return new EssayPolishResponse(
                part,
                StringUtils.hasText(abs) ? abs : req.abstractText(),
                StringUtils.hasText(body) ? body : req.bodyText(),
                raw);
    }

    private EssayScoreResponse parseScore(String raw) {
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
                // 维度分齐全时以维度合计为准，并限制在 0～75
                if (sum > 0) {
                    total = sum;
                }
            }
            total = Math.max(0, Math.min(75, total));
            String level = resolvePassLevel(total);
            List<String> strengths = readStringList(root.path("strengths"));
            List<String> improvements = readStringList(root.path("improvements"));
            String summary = root.path("summary").asText("");
            return new EssayScoreResponse(total, level, dims, summary, strengths, improvements, raw);
        } catch (Exception e) {
            // JSON 解析失败时仍返回原始文本，方便前端展示
            return new EssayScoreResponse(
                    0,
                    "不及格",
                    List.of(),
                    "模型返回内容无法解析为标准 JSON，请查看 raw。",
                    List.of(),
                    List.of(),
                    raw);
        }
    }

    private EssayGuideResponse parseGuide(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<EssayGuideSection> framework = new ArrayList<>();
            JsonNode arr = root.path("framework");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    framework.add(new EssayGuideSection(
                            n.path("name").asText(""),
                            n.path("words").asText(""),
                            n.path("content").asText("")));
                }
            }
            JsonNode p = root.path("project");
            EssayProjectExample project = new EssayProjectExample(
                    p.path("name").asText(""),
                    p.path("industry").asText(""),
                    p.path("company").asText(""),
                    p.path("role").asText(""),
                    p.path("period").asText(""),
                    p.path("background").asText(""),
                    p.path("modules").asText(""),
                    p.path("techChoice").asText(""),
                    p.path("effects").asText(""),
                    p.path("story").asText(""));
            return new EssayGuideResponse(
                    root.path("recognizedTopic").asText(""),
                    readStringList(root.path("subQuestions")),
                    readStringList(root.path("coreArguments")),
                    root.path("timePlan").asText(""),
                    framework,
                    readStringList(root.path("tips")),
                    readStringList(root.path("pitfalls")),
                    project,
                    root.path("abstractDraft").asText(""),
                    root.path("bodyOutline").asText(""),
                    raw);
        } catch (Exception e) {
            return new EssayGuideResponse(
                    "",
                    List.of(),
                    List.of(),
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    new EssayProjectExample("", "", "", "", "", "", "", "", "", ""),
                    "",
                    raw,
                    raw);
        }
    }

    /** 软考论文：满分 75；≥45 合格，&lt;45 不及格 */
    private static String resolvePassLevel(int totalScore) {
        return totalScore >= 45 ? "合格" : "不及格";
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

    private static String extract(Pattern pattern, String raw) {
        Matcher m = pattern.matcher(raw);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private static String stripMarkers(String raw) {
        return raw.replaceAll("【摘要】", "")
                .replaceAll("【正文】", "")
                .trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
