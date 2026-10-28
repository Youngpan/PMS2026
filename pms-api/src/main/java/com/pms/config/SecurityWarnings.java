package com.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动安全告警：检测仍在使用默认值的敏感配置，大声警告。
 * 不阻断启动（便于首次快速试用），但生产部署必须消除这些告警。
 */
@Slf4j
@Order(0)
@Component
public class SecurityWarnings implements CommandLineRunner {

    private static final String DEFAULT_JWT_SECRET =
            "change-me-to-a-long-random-secret-string-at-least-32-chars";

    @Value("${pms.jwt.secret}")
    private String jwtSecret;

    @Value("${pms.default-admin.password:admin}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (DEFAULT_JWT_SECRET.equals(jwtSecret) || jwtSecret == null || jwtSecret.length() < 32) {
            log.warn("============================================================");
            log.warn("  ⚠️  安全告警：JWT 密钥仍为默认占位值或长度不足 32 字符。");
            log.warn("      生产环境必须通过环境变量 JWT_SECRET 设置足够长的随机串。");
            log.warn("============================================================");
        }
        if ("admin".equals(defaultAdminPassword)) {
            log.warn("============================================================");
            log.warn("  ⚠️  安全告警：admin 默认密码仍为 'admin'。");
            log.warn("      生产环境请通过 ADMIN_PASSWORD 设置强密码，");
            log.warn("      或启动后立即调用 PUT /api/v1/staff/1/password 修改。");
            log.warn("============================================================");
        }
    }
}
