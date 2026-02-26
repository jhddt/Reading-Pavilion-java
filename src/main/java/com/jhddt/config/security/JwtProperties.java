package com.jhddt.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * 从 application.yml 中读取配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * JWT 签名密钥（至少32位）
     */
    private String secret = "your-secret-key-must-be-at-least-256-bits-long-for-hs256";

    /**
     * Token 过期时间（毫秒），默认7天
     */
    private Long expiration = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token 请求头名称
     */
    private String headerName = "Authorization";
}
