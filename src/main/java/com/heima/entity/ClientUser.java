package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ClientUser {
    private Long id;
    private String email;
    private String name;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
