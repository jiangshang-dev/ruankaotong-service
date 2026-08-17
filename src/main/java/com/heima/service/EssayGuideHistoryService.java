package com.heima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryRecord;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 论文指导历史：按科目 + 论文文件名存在 Redis List，最新在前。
 */
@Slf4j
@Service
public class EssayGuideHistoryService {

    static final String KEY_PREFIX = "ruankao:essay-guide:";
    static final int MAX_RECORDS = 20;
    static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public EssayGuideHistoryService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(EssayGuideHistoryRecord record) {
        if (record == null || !StringUtils.hasText(record.markdown())) {
            return;
        }
        String key = key(record.subjectId(), record.fileName());
        try {
            redis.opsForList().leftPush(key, objectMapper.writeValueAsString(record));
            redis.opsForList().trim(key, 0, MAX_RECORDS - 1);
            redis.expire(key, TTL);
        } catch (Exception e) {
            log.warn("保存论文指导历史失败 key={}", key, e);
        }
    }

    public EssayGuideHistoryResponse list(String subjectId, String fileName) {
        String key = key(subjectId, fileName);
        List<EssayGuideHistoryRecord> records = new ArrayList<>();
        try {
            List<String> raw = redis.opsForList().range(key, 0, MAX_RECORDS - 1);
            if (raw != null) {
                for (String item : raw) {
                    if (!StringUtils.hasText(item)) {
                        continue;
                    }
                    try {
                        records.add(objectMapper.readValue(item, EssayGuideHistoryRecord.class));
                    } catch (Exception parseEx) {
                        log.warn("解析论文指导历史失败 key={}", key, parseEx);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取论文指导历史失败 key={}", key, e);
        }
        return new EssayGuideHistoryResponse(normalize(subjectId, "_"), normalize(fileName, "_unsaved"), records);
    }

    static String key(String subjectId, String fileName) {
        return KEY_PREFIX + normalize(subjectId, "_") + ":" + normalize(fileName, "_unsaved");
    }

    static String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
