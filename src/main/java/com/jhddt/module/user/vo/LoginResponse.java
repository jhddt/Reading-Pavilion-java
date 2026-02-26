package com.jhddt.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应结果
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户角色
     */
    private Integer role;
}
