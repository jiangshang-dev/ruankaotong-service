package com.heima.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.mail.MailUtil;
import com.heima.config.MailProperties;
import com.heima.dto.ClientAuthDtos.ClientLoginResponse;
import com.heima.entity.ClientSession;
import com.heima.entity.ClientUser;
import com.heima.entity.EmailCode;
import com.heima.mapper.ClientSessionMapper;
import com.heima.mapper.ClientUserMapper;
import com.heima.mapper.EmailCodeMapper;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ClientAuthService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final MailProperties mailProperties;
    private final EmailCodeMapper emailCodeMapper;
    private final ClientUserMapper clientUserMapper;
    private final ClientSessionMapper clientSessionMapper;

    public ClientAuthService(
            MailProperties mailProperties,
            EmailCodeMapper emailCodeMapper,
            ClientUserMapper clientUserMapper,
            ClientSessionMapper clientSessionMapper) {
        this.mailProperties = mailProperties;
        this.emailCodeMapper = emailCodeMapper;
        this.clientUserMapper = clientUserMapper;
        this.clientSessionMapper = clientSessionMapper;
    }

    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (emailCodeMapper.countRecent(email, 60) > 0) {
            throw new IllegalArgumentException("验证码已发送，请稍后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        String html = "<p>你正在登录软考通 AI 辅导。</p><p>验证码：<b style=\"font-size:20px\">"
                + code + "</b></p><p>5 分钟内有效，如非本人操作请忽略。</p>";
        try {
            MailUtil.send(mailProperties.toAccount(), email, "软考通登录验证码", html, true);
        } catch (Exception e) {
            log.error("发送验证码失败 email={}", email, e);
            throw new IllegalArgumentException("验证码发送失败，请检查邮箱或稍后重试");
        }
        EmailCode row = new EmailCode();
        row.setEmail(email);
        row.setCode(code);
        row.setExpireAt(LocalDateTime.now().plusMinutes(5));
        emailCodeMapper.insert(row);
    }

    public ClientLoginResponse login(String rawEmail, String code) {
        String email = normalize(rawEmail);
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("请填写验证码");
        }
        EmailCode latest = emailCodeMapper.selectLatest(email);
        if (latest == null || !code.trim().equals(latest.getCode())) {
            throw new IllegalArgumentException("验证码不对或已过期");
        }
        emailCodeMapper.markUsed(latest.getId());
        ClientUser user = clientUserMapper.selectByEmail(email);
        if (user == null) {
            user = new ClientUser();
            user.setEmail(email);
            user.setName(email);
            clientUserMapper.insert(user);
        } else {
            clientUserMapper.touchLogin(user.getId());
        }
        clientSessionMapper.deleteExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        ClientSession session = new ClientSession();
        session.setClientId(user.getId());
        session.setToken(token);
        session.setExpireAt(LocalDateTime.now().plusDays(30));
        clientSessionMapper.insert(session);
        return new ClientLoginResponse(token, user.getEmail(), user.getName());
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            clientSessionMapper.deleteByToken(token.trim());
        }
    }

    public ClientUser findByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        ClientSession session = clientSessionMapper.selectByToken(token.trim());
        if (session == null) {
            return null;
        }
        return clientUserMapper.selectById(session.getClientId());
    }

    public static String normalize(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("请填写正确的邮箱");
        }
        return email;
    }
}
