package com.jhddt.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhddt.common.result.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义访问拒绝处理器
 * 当用户已认证但没有权限访问资源时触发
 * 返回 403 Forbidden
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 设置响应状态码为 403
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        // 设置响应内容类型为 JSON
        response.setContentType("application/json;charset=UTF-8");

        // 构建统一的错误响应
        Result<Void> result = Result.error(403, "权限不足，无法访问");

        // 将结果写入响应
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
