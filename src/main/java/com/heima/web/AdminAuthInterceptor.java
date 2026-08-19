package com.heima.web;

import com.heima.entity.AdminSession;
import com.heima.entity.AdminUser;
import com.heima.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN = "rkAdmin";

    private final AdminAuthService adminAuthService;

    public AdminAuthInterceptor(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith("/login")) {
            return true;
        }
        String token = bearer(request);
        AdminSession session = adminAuthService.findValidSession(token);
        if (session == null) {
            writeUnauthorized(response);
            return false;
        }
        AdminUser admin = adminAuthService.findAdmin(session.getAdminId());
        if (admin == null) {
            writeUnauthorized(response);
            return false;
        }
        request.setAttribute(ATTR_ADMIN, admin);
        return true;
    }

    private static void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"请先登录\"}");
    }

    private static String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return "";
    }
}
