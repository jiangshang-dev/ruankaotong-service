package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ClientSession {
    private Long id;
    private Long clientId;
    private String token;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
