package com.heima.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.config.AiProperties;
import com.heima.dto.EssayAiDtos.EssayPolishRequest;
import com.heima.dto.EssayAiDtos.EssayPolishResponse;
import com.heima.dto.EssayAiDtos.EssayScoreRequest;
import com.heima.dto.EssayAiDtos.EssayScoreResponse;
import com.heima.dto.EssayAiDtos.ScoreDimension;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 软考论文 AI：每次请求新建 ReActAgent，不挂 session / longTermMemory，无历史落盘。
 */
@Service
public class EssayAiService {

    private static final Pattern ABSTRACT_BLOCK =
            Pattern.compile("【摘要】\\s*([\\s\\S]*?)(?=【正文】|$)");
    private static final Pattern BODY_BLOCK =
            Pattern.compile("【正文】\\s*([\\s\\S]*)$");

    private final AiProperties props;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    public EssayAiService(AiProperties props, PromptLoader promptLoader, ObjectMapper objectMapper) {
        this.props = props;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
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

        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userText)
                .build();

        Msg response = agent.call(msg)
                .block(Duration.ofSeconds(Math.max(30, props.getTimeoutSeconds())));
        if (response == null || !StringUtils.hasText(response.getTextContent())) {
            throw new IllegalStateException("模型未返回有效内容");
        }
        return response.getTextContent().trim();
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
