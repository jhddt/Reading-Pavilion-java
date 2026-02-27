package com.jhddt.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求参数
 */
@Schema(description = "登录请求参数")
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zrk1", required = true)
    @JsonProperty("username")  // 支持 username 和 userName 两种格式
    private String userName;

    /**
     * 密码（明文）
     */
    @Schema(description = "密码", example = "123456", required = true)
    private String password;
}
