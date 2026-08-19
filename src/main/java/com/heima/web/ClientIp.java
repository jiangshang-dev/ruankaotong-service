package com.heima.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIp {

    private ClientIp() {
    }

    public static String from(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = header(request, "X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String real = header(request, "X-Real-IP");
        if (StringUtils.hasText(real)) {
            return real;
        }
        String ip = request.getRemoteAddr();
        if ("https://example.net/id/garnet".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value.trim();
    }
}
