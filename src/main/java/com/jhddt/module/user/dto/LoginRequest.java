package com.jhddt.module.user.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码（明文）
     */
    private String password;
}
