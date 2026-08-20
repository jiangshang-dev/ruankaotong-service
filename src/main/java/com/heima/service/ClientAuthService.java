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
    private static final int NAME_MAX = 16;

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
            user.setName("考生");
            user.setEnabled(1);
            clientUserMapper.insert(user);
            assignDefaultName(user);
        } else {
            if (!isEnabled(user)) {
                throw new IllegalArgumentException("账号已禁用，请联系管理员");
            }
            if (needsDefaultName(user.getName(), user.getEmail())) {
                assignDefaultName(user);
            }
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

    public ClientLoginResponse updateName(ClientUser user, String rawName) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("请先登录后再使用 AI");
        }
        if (!isEnabled(user)) {
            throw new IllegalArgumentException("账号已禁用，请联系管理员");
        }
        String name = normalizeName(rawName);
        clientUserMapper.updateName(user.getId(), name);
        user.setName(name);
        return new ClientLoginResponse("", user.getEmail(), user.getName());
    }

    public void setEnabled(Long id, boolean enabled) {
        if (id == null) {
            throw new IllegalArgumentException("缺少用户 ID");
        }
        ClientUser user = clientUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("找不到该用户");
        }
        clientUserMapper.updateEnabled(id, enabled ? 1 : 0);
        if (!enabled) {
            clientSessionMapper.deleteByClientId(id);
        }
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

    public static boolean isEnabled(ClientUser user) {
        if (user == null) {
            return false;
        }
        Integer enabled = user.getEnabled();
        return enabled == null || enabled == 1;
    }

    public static String defaultNickname(Long id) {
        long n = id == null ? 0 : id;
        return "考生" + String.format("%04d", n);
    }

    public static String normalize(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("请填写正确的邮箱");
        }
        return email;
    }

    private void assignDefaultName(ClientUser user) {
        user.setName(defaultNickname(user.getId()));
        clientUserMapper.updateName(user.getId(), user.getName());
    }

    private static boolean needsDefaultName(String name, String email) {
        if (!StringUtils.hasText(name) || "考生".equals(name.trim())) {
            return true;
        }
        return email != null && email.equalsIgnoreCase(name.trim());
    }

    private static String normalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", "");
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("请填写昵称");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("昵称最多 " + NAME_MAX + " 个字");
        }
        return name;
    }
}
