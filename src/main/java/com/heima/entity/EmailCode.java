package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EmailCode {
    private Long id;
    private String email;
    private String code;
    private LocalDateTime expireAt;
    private Integer used;
    private LocalDateTime createdAt;
}
