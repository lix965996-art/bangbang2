package com.farmland.intel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时校验关键配置项，防止生产环境遗漏敏感配置。
 */
@Component
public class ConfigValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Override
    public void run(String... args) {
        validateJwtSecret();
        validateDbPassword();
        log.info("配置校验通过");
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            String msg = "严重配置错误: jwt.secret 未设置！"
                    + "请通过环境变量 JWT_SECRET 设置一个至少 32 字节的密钥。"
                    + "示例: export JWT_SECRET=your-secure-secret-key-at-least-32-chars";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        if (jwtSecret.length() < 32) {
            String msg = "配置警告: jwt.secret 长度不足 32 字节，安全性较低。"
                    + "当前长度: " + jwtSecret.length() + "，建议至少 32 字节。";
            log.warn(msg);
        }
    }

    private void validateDbPassword() {
        // 检查是否为非 localhost 的数据库连接（生产环境）
        boolean isLocalhost = dbUrl == null
                || dbUrl.contains("localhost")
                || dbUrl.contains("127.0.0.1");

        if (!isLocalhost && (dbPassword == null || dbPassword.trim().isEmpty())) {
            String msg = "严重配置错误: 连接非本地数据库时 spring.datasource.password 不能为空！"
                    + "请通过环境变量 DB_PASSWORD 设置数据库密码。";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (dbPassword != null && !dbPassword.isEmpty() && dbPassword.length() < 6) {
            log.warn("配置警告: 数据库密码长度不足 6 位，安全性较低。");
        }
    }
}
