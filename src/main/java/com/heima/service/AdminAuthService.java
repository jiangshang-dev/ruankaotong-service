package com.heima.service;

import com.heima.dto.AdminDtos.LoginResponse;
import com.heima.entity.AdminSession;
import com.heima.entity.AdminUser;
import com.heima.mapper.AdminSessionMapper;
import com.heima.mapper.AdminUserMapper;
import com.heima.web.AdminAuthException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            AdminSessionMapper adminSessionMapper,
            PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("请填写账号和密码");
        }
        AdminUser admin = adminUserMapper.selectByAccount(account.trim());
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new IllegalArgumentException("账号或密码不对");
        }
        adminSessionMapper.deleteExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        AdminSession session = new AdminSession();
        session.setAdminId(admin.getId());
        session.setToken(token);
        session.setExpireAt(LocalDateTime.now().plusDays(7));
        adminSessionMapper.insert(session);
        return new LoginResponse(token, admin.getAccount(), admin.getName());
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            adminSessionMapper.deleteByToken(token);
        }
    }

    public AdminSession findValidSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return adminSessionMapper.selectByToken(token.trim());
    }

    public AdminUser findAdmin(Long id) {
        return id == null ? null : adminUserMapper.selectById(id);
    }

    public void changePassword(AdminUser admin, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("请填写原密码和新密码");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        AdminUser db = adminUserMapper.selectById(admin.getId());
        if (db == null || !passwordEncoder.matches(oldPassword, db.getPassword())) {
            throw new IllegalArgumentException("原密码不对");
        }
        adminUserMapper.updatePassword(admin.getId(), passwordEncoder.encode(newPassword));
    }

    public String requireToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        throw new AdminAuthException("请先登录");
    }
}
