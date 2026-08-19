package com.heima.service;

import com.heima.dto.AdminDtos.AiQaQuery;
import com.heima.dto.AdminDtos.ClientIpRow;
import com.heima.dto.AdminDtos.DashboardStats;
import com.heima.dto.AdminDtos.PageResult;
import com.heima.entity.AiQaLog;
import com.heima.mapper.AiQaMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AiQaLogService {

    private static final int QUESTION_MAX = 4000;
    private static final int ANSWER_MAX = 50_000;

    private final AiQaMapper aiQaMapper;

    public AiQaLogService(AiQaMapper aiQaMapper) {
        this.aiQaMapper = aiQaMapper;
    }

    public void record(
            String clientIp,
            String module,
            String action,
            String subjectId,
            String subjectName,
            String fileName,
            String topic,
            String question,
            String answer,
            String email) {
        try {
            AiQaLog row = new AiQaLog();
            row.setClientIp(StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown");
            row.setModule(empty(module, "其它"));
            row.setAction(empty(action, "调用"));
            row.setSubjectId(clip(subjectId, 64));
            row.setSubjectName(clip(subjectName, 128));
            row.setFileName(clip(fileName, 255));
            row.setTopic(clip(topic, 255));
            row.setQuestion(clip(question, QUESTION_MAX));
            row.setAnswer(clip(answer, ANSWER_MAX));
            row.setEmail(clip(email, 128));
            aiQaMapper.insert(row);
        } catch (Exception e) {
            log.warn("记录 AI 问答失败 ip={} module={} action={}", clientIp, module, action, e);
        }
    }

    public DashboardStats dashboard() {
        DashboardStats stats = aiQaMapper.selectDashboard();
        return stats == null ? new DashboardStats() : stats;
    }

    public List<ClientIpRow> clients(String ip) {
        return aiQaMapper.selectClients(ip);
    }

    public PageResult<AiQaLog> page(int page, int size, AiQaQuery query) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        AiQaQuery q = query == null ? new AiQaQuery("", "", "", "") : query;
        long total = aiQaMapper.countList(q);
        List<AiQaLog> records = total == 0
                ? List.of()
                : aiQaMapper.selectList(q, (long) (p - 1) * s, s);
        return new PageResult<>(total, p, s, records);
    }

    public AiQaLog detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少记录 ID");
        }
        AiQaLog row = aiQaMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("找不到这条问答");
        }
        return row;
    }

    public List<AiQaLog> recent(int limit) {
        return aiQaMapper.selectRecent(Math.min(Math.max(limit, 1), 20));
    }

    private static String empty(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String clip(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
