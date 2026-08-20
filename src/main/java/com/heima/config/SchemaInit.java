package com.heima.config;

import com.heima.entity.AdminUser;
import com.heima.entity.RkSubject;
import com.heima.mapper.AdminUserMapper;
import com.heima.mapper.RkSubjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SchemaInit implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final AdminUserMapper adminUserMapper;
    private final RkSubjectMapper subjectMapper;
    private final PasswordEncoder passwordEncoder;

    public SchemaInit(
            JdbcTemplate jdbcTemplate,
            AdminUserMapper adminUserMapper,
            RkSubjectMapper subjectMapper,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminUserMapper = adminUserMapper;
        this.subjectMapper = subjectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_admin (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  account VARCHAR(64) NOT NULL,
                  password VARCHAR(128) NOT NULL,
                  name VARCHAR(64) NOT NULL DEFAULT '管理员',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_account (account)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_admin_session (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  admin_id BIGINT NOT NULL,
                  token VARCHAR(64) NOT NULL,
                  expire_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_token (token),
                  KEY idx_admin_id (admin_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_subject (
                  id VARCHAR(64) NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  short_name VARCHAR(32) NOT NULL,
                  level VARCHAR(16) NOT NULL,
                  color VARCHAR(16) NOT NULL DEFAULT '#0f766e',
                  sort_no INT NOT NULL DEFAULT 0,
                  enabled TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_ai_qa (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  client_ip VARCHAR(64) NOT NULL,
                  module VARCHAR(16) NOT NULL,
                  action VARCHAR(32) NOT NULL,
                  subject_id VARCHAR(64) DEFAULT NULL,
                  subject_name VARCHAR(128) DEFAULT NULL,
                  file_name VARCHAR(255) DEFAULT NULL,
                  topic VARCHAR(255) DEFAULT NULL,
                  question MEDIUMTEXT,
                  answer MEDIUMTEXT,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_ip_time (client_ip, created_at),
                  KEY idx_module_time (module, created_at),
                  KEY idx_created (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_client (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  email VARCHAR(128) NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  enabled TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  last_login_at DATETIME DEFAULT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_email (email)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_client_session (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  client_id BIGINT NOT NULL,
                  token VARCHAR(64) NOT NULL,
                  expire_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_token (token),
                  KEY idx_client_id (client_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rk_email_code (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  email VARCHAR(128) NOT NULL,
                  code VARCHAR(16) NOT NULL,
                  expire_at DATETIME NOT NULL,
                  used TINYINT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_email_time (email, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        addColumnIfMissing("rk_ai_qa", "email",
                "ALTER TABLE rk_ai_qa ADD COLUMN email VARCHAR(128) DEFAULT NULL AFTER answer");
        addColumnIfMissing("rk_client", "enabled",
                "ALTER TABLE rk_client ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 AFTER name");
        jdbcTemplate.update(
                "UPDATE rk_client SET name = CONCAT('考生', LPAD(id, 4, '0')) WHERE name = email OR name = '' OR name = '考生'");
        if (adminUserMapper.countAll() == 0) {
            AdminUser admin = new AdminUser();
            admin.setAccount("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("管理员");
            adminUserMapper.insert(admin);
            log.info("已初始化管理员账号 admin / admin123");
        }
        if (subjectMapper.countAll() == 0) {
            seedSubjects();
            log.info("已初始化默认学科");
        }
    }

    private void seedSubjects() {
        List<RkSubject> rows = List.of(
                subject("architect", "系统架构设计师", "架构", "高级", "#0f766e", 1),
                subject("pm", "信息系统项目管理师", "高项", "高级", "#1d4ed8", 2),
                subject("analyst", "系统分析师", "分析", "高级", "#7c2d12", 3),
                subject("network-planner", "网络规划设计师", "网规", "高级", "#4338ca", 4),
                subject("se", "软件设计师", "软设", "中级", "#047857", 5),
                subject("network", "网络工程师", "网工", "中级", "#0369a1", 6),
                subject("db", "数据库系统工程师", "库工", "中级", "#a16207", 7),
                subject("info-security", "信息安全工程师", "安工", "中级", "#be123c", 8),
                subject("media", "多媒体应用设计师", "多媒", "中级", "#c2410c", 9),
                subject("programmer", "程序员", "程序", "初级", "#475569", 10),
                subject("network-admin", "网络管理员", "网管", "初级", "#64748b", 11)
        );
        rows.forEach(subjectMapper::insert);
    }

    private static RkSubject subject(
            String id, String name, String shortName, String level, String color, int sortNo) {
        RkSubject row = new RkSubject();
        row.setId(id);
        row.setName(name);
        row.setShortName(shortName);
        row.setLevel(level);
        row.setColor(color);
        row.setSortNo(sortNo);
        row.setEnabled(1);
        return row;
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        Integer n = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """,
                Integer.class,
                table,
                column);
        if (n == null || n == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
