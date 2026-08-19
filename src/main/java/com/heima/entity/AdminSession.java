package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminSession {
    private Long id;
    private Long adminId;
    private String token;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
