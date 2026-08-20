package com.heima.controller;

import com.heima.dto.ClientAuthDtos.ClientLoginResponse;
import com.heima.dto.ClientAuthDtos.EmailLoginRequest;
import com.heima.dto.ClientAuthDtos.SendCodeRequest;
import com.heima.dto.ClientAuthDtos.UpdateProfileRequest;
import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.entity.ClientUser;
import com.heima.service.ClientAuthService;
import com.heima.web.ClientAuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户端登录", description = "邮箱验证码登录，仅 AI 功能需要")
@RestController
@RequestMapping("/api/auth")
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    public ClientAuthController(ClientAuthService clientAuthService) {
        this.clientAuthService = clientAuthService;
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/code")
    public ApiError sendCode(@RequestBody SendCodeRequest request) {
        clientAuthService.sendCode(request == null ? "" : request.email());
        return new ApiError("ok");
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login")
    public ClientLoginResponse login(@RequestBody EmailLoginRequest request) {
        return clientAuthService.login(
                request == null ? "" : request.email(),
                request == null ? "" : request.code());
    }

    @GetMapping("/me")
    public ClientLoginResponse me(HttpServletRequest request) {
        ClientUser user = (ClientUser) request.getAttribute(ClientAuthInterceptor.ATTR_CLIENT);
        if (user == null) {
            String header = request.getHeader("Authorization");
            String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : "";
            user = clientAuthService.findByToken(token);
        }
        if (user == null) {
            throw new IllegalArgumentException("请先登录后再使用 AI");
        }
        if (!ClientAuthService.isEnabled(user)) {
            throw new IllegalArgumentException("账号已禁用，请联系管理员");
        }
        return new ClientLoginResponse("", user.getEmail(), user.getName());
    }

    @PostMapping("/profile")
    public ClientLoginResponse profile(HttpServletRequest request, @RequestBody UpdateProfileRequest body) {
        ClientUser user = (ClientUser) request.getAttribute(ClientAuthInterceptor.ATTR_CLIENT);
        if (user == null) {
            String header = request.getHeader("Authorization");
            String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : "";
            user = clientAuthService.findByToken(token);
        }
        return clientAuthService.updateName(user, body == null ? "" : body.name());
    }

    @PostMapping("/logout")
    public ApiError logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : "";
        clientAuthService.logout(token);
        return new ApiError("ok");
    }
}
