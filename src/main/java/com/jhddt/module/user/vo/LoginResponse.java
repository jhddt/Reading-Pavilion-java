package com.jhddt.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应结果
 */
@Schema(description = "登录响应结果")
@Data
@AllArgsConstructor
public class LoginResponse {
    /**
     * JWT Token
     */
    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zrk1")
    private String userName;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色：1-学生，2-教师，3-管理员", example = "1")
    private Integer role;
}
