package com.heima.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record LoginRequest(String account, String password) {
    }

    public record LoginResponse(String token, String account, String name) {
    }

    public record PasswordRequest(String oldPassword, String newPassword) {
    }

    public record SubjectSaveRequest(
            String id,
            String name,
            String shortName,
            String level,
            String color,
            Integer sortNo,
            Boolean enabled
    ) {
    }

    public record SubjectToggleRequest(String id, Boolean enabled) {
    }

    public record AiQaQuery(
            String ip,
            String module,
            String subject,
            String keyword
    ) {
    }

    @Schema(description = "概览数字")
    @lombok.Data
    public static class DashboardStats {
        private Long qaToday;
        private Long qaTotal;
        private Long ips;
        private Long ipsToday;
        private Long subjects;
    }

    @lombok.Data
    public static class ClientIpRow {
        private String ip;
        private LocalDateTime lastSeen;
        private Long count;
        private String modules;
        private String subject;
        private String email;
    }

    public record PageResult<T>(long total, long page, long size, List<T> records) {
    }
}
