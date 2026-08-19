package com.heima.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RkSubject {
    private String id;
    private String name;
    private String shortName;
    private String level;
    private String color;
    private Integer sortNo;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
