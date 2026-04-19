package com.jhddt.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 统一获取当前登录用户上下文，减少 Controller 中重复转换逻辑。
 */
@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return id(authentication);
    }

    public Long id(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("当前请求未认证");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new IllegalStateException("无法解析当前用户ID");
    }
}
