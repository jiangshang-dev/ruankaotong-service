package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiQaLog {
    private Long id;
    private String clientIp;
    private String module;
    private String action;
    private String subjectId;
    private String subjectName;
    private String fileName;
    private String topic;
    private String question;
    private String answer;
    private String email;
    private LocalDateTime createdAt;
}
