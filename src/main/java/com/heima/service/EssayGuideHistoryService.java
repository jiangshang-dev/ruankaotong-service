package com.heima.service;

import com.heima.dto.EssayAiDtos.EssayGuideHistoryRecord;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 论文指导历史：只读 AgentScope 默认落盘的 {@code agent_state}，不另写 Redis List。
 */
@Slf4j
@Service
public class EssayGuideHistoryService {

    static final String AGENT_STATE_KEY = "agent_state";
    private static final DateTimeFormatter MSG_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final AgentStateStore stateStore;

    public EssayGuideHistoryService(AgentStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public EssayGuideHistoryResponse list(String subjectId, String fileName) {
        return list(subjectId, fileName, "");
    }

    public EssayGuideHistoryResponse list(String subjectId, String fileName, String sessionPrefix) {
        String userId = userId(subjectId);
        String sessionId = sessionId(fileName, sessionPrefix);
        List<EssayGuideHistoryRecord> records = new ArrayList<>();
        try {
            AgentState state = stateStore.get(userId, sessionId, AGENT_STATE_KEY, AgentState.class)
                    .orElse(null);
            if (state != null && state.getContext() != null) {
                records.addAll(fromContext(userId, sessionId, state.getContext()));
            }
        } catch (Exception e) {
            log.warn("读取论文指导 agent_state 失败 userId={} sessionId={}", userId, sessionId, e);
        }
        Collections.reverse(records);
        return new EssayGuideHistoryResponse(userId, sessionId, records);
    }

    static String userId(String subjectId) {
        return normalize(subjectId, "_");
    }

    static String sessionId(String fileName) {
        return sessionId(fileName, "");
    }

    static String sessionId(String fileName, String prefix) {
        String name = normalize(fileName, "_unsaved").replace(':', '_').replace('/', '_');
        if (!StringUtils.hasText(prefix)) {
            return name;
        }
        return prefix.replace(':', '_').replace('/', '_') + "_" + name;
    }

    static String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static List<EssayGuideHistoryRecord> fromContext(
            String subjectId, String fileName, List<Msg> context) {
        List<EssayGuideHistoryRecord> records = new ArrayList<>();
        for (Msg msg : context) {
            if (msg == null || msg.getRole() != MsgRole.ASSISTANT) {
                continue;
            }
            String markdown = textOf(msg);
            String thinking = thinkingOf(msg);
            if (!StringUtils.hasText(markdown) && !StringUtils.hasText(thinking)) {
                continue;
            }
            records.add(new EssayGuideHistoryRecord(
                    StringUtils.hasText(msg.getId()) ? msg.getId() : String.valueOf(records.size()),
                    parseTimestamp(msg.getTimestamp()),
                    subjectId,
                    fileName,
                    headingOf(markdown),
                    markdown,
                    thinking));
        }
        return records;
    }

    private static String textOf(Msg msg) {
        return msg.getTextContent() == null ? "" : msg.getTextContent().trim();
    }

    private static String thinkingOf(Msg msg) {
        List<ContentBlock> blocks = msg.getContent();
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (ContentBlock block : blocks) {
            if (block instanceof ThinkingBlock thinkingBlock) {
                if (StringUtils.hasText(thinkingBlock.getThinking())) {
                    if (!out.isEmpty()) {
                        out.append("\n\n");
                    }
                    out.append(thinkingBlock.getThinking().trim());
                }
            }
        }
        return out.toString();
    }

    private static String headingOf(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        for (String line : markdown.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return "";
    }

    private static long parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return 0L;
        }
        try {
            return LocalDateTime.parse(timestamp.trim(), MSG_TIME)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
