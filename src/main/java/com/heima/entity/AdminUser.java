package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminUser {
    private Long id;
    private String account;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
