package com.heima.controller;

import com.heima.dto.AdminDtos.AiQaQuery;
import com.heima.dto.AdminDtos.ClientIpRow;
import com.heima.dto.AdminDtos.ClientUserRow;
import com.heima.dto.AdminDtos.ClientUserToggleRequest;
import com.heima.dto.AdminDtos.DashboardStats;
import com.heima.dto.AdminDtos.LoginRequest;
import com.heima.dto.AdminDtos.LoginResponse;
import com.heima.dto.AdminDtos.PageResult;
import com.heima.dto.AdminDtos.PasswordRequest;
import com.heima.dto.AdminDtos.SubjectSaveRequest;
import com.heima.dto.AdminDtos.SubjectToggleRequest;
import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.entity.AdminUser;
import com.heima.entity.AiQaLog;
import com.heima.entity.RkSubject;
import com.heima.mapper.ClientUserMapper;
import com.heima.service.AdminAuthService;
import com.heima.service.AiQaLogService;
import com.heima.service.ClientAuthService;
import com.heima.service.SubjectAdminService;
import com.heima.web.AdminAuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台", description = "管理员登录、学科、客户端 IP 与 AI 问答记录")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final SubjectAdminService subjectAdminService;
    private final AiQaLogService aiQaLogService;
    private final ClientUserMapper clientUserMapper;
    private final ClientAuthService clientAuthService;

    public AdminController(
            AdminAuthService adminAuthService,
            SubjectAdminService subjectAdminService,
            AiQaLogService aiQaLogService,
            ClientUserMapper clientUserMapper,
            ClientAuthService clientAuthService) {
        this.adminAuthService = adminAuthService;
        this.subjectAdminService = subjectAdminService;
        this.aiQaLogService = aiQaLogService;
        this.clientUserMapper = clientUserMapper;
        this.clientAuthService = clientAuthService;
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return adminAuthService.login(
                request == null ? "" : request.account(),
                request == null ? "" : request.password());
    }

    @PostMapping("/logout")
    public ApiError logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            adminAuthService.logout(adminAuthService.requireToken(authorization));
        } catch (Exception ignored) {
            // 已退出也当成功
        }
        return new ApiError("ok");
    }

    @GetMapping("/me")
    public LoginResponse me(HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return new LoginResponse("", admin.getAccount(), admin.getName());
    }

    @PostMapping("/password")
    public ApiError password(HttpServletRequest request, @RequestBody PasswordRequest body) {
        AdminUser admin = (AdminUser) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        adminAuthService.changePassword(
                admin,
                body == null ? "" : body.oldPassword(),
                body == null ? "" : body.newPassword());
        return new ApiError("ok");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        DashboardStats stats = aiQaLogService.dashboard();
        List<AiQaLog> recentQa = aiQaLogService.recent(5);
        List<ClientIpRow> recentIp = aiQaLogService.clients("");
        if (recentIp.size() > 5) {
            recentIp = recentIp.subList(0, 5);
        }
        return Map.of(
                "stats", stats,
                "recentQa", recentQa,
                "recentIp", recentIp
        );
    }

    @GetMapping("/clients")
    public List<ClientIpRow> clients(@RequestParam(required = false, defaultValue = "") String ip) {
        return aiQaLogService.clients(ip);
    }

    @GetMapping("/users")
    public List<ClientUserRow> users(@RequestParam(required = false, defaultValue = "") String q) {
        return clientUserMapper.selectAdminList(q == null ? "" : q.trim());
    }

    @PostMapping("/users/toggle")
    public ApiError toggleUser(@RequestBody ClientUserToggleRequest body) {
        clientAuthService.setEnabled(
                body == null ? null : body.id(),
                body != null && Boolean.TRUE.equals(body.enabled()));
        return new ApiError("ok");
    }

    @GetMapping("/qa")
    public PageResult<AiQaLog> qa(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "") String ip,
            @RequestParam(required = false, defaultValue = "") String module,
            @RequestParam(required = false, defaultValue = "") String subject,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return aiQaLogService.page(page, size, new AiQaQuery(ip, module, subject, keyword));
    }

    @GetMapping("/qa/{id}")
    public AiQaLog qaDetail(@PathVariable Long id) {
        return aiQaLogService.detail(id);
    }

    @GetMapping("/subjects")
    public List<RkSubject> subjects() {
        return subjectAdminService.listAll();
    }

    @PostMapping("/subjects")
    public RkSubject createSubject(@RequestBody SubjectSaveRequest body) {
        return subjectAdminService.save(body, true);
    }

    @PostMapping("/subjects/update")
    public RkSubject updateSubject(@RequestBody SubjectSaveRequest body) {
        return subjectAdminService.save(body, false);
    }

    @PostMapping("/subjects/toggle")
    public RkSubject toggleSubject(@RequestBody SubjectToggleRequest body) {
        return subjectAdminService.toggle(body == null ? null : body.id(), body == null ? null : body.enabled());
    }
}
