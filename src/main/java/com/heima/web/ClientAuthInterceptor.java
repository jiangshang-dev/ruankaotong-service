package com.heima.web;

import com.heima.entity.ClientUser;
import com.heima.service.ClientAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClientAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_CLIENT = "rkClient";

    private final ClientAuthService clientAuthService;

    public ClientAuthInterceptor(ClientAuthService clientAuthService) {
        this.clientAuthService = clientAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith("/health")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = "";
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        ClientUser user = clientAuthService.findByToken(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"请先登录后再使用 AI\"}");
            return false;
        }
        request.setAttribute(ATTR_CLIENT, user);
        return true;
    }

    public static String email(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_CLIENT);
        if (value instanceof ClientUser user) {
            return user.getEmail();
        }
        return "";
    }
}
