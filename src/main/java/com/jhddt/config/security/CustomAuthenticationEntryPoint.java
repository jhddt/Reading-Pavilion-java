package com.jhddt.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhddt.common.result.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义认证入口点
 * 当用户未认证（没有 Token 或 Token 无效）访问需要认证的资源时触发
 * 返回 401 Unauthorized
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 设置响应状态码为 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 设置响应内容类型为 JSON
        response.setContentType("application/json;charset=UTF-8");

        // 构建统一的错误响应
        Result<Void> result = Result.error(401, "未认证，请先登录");

        // 将结果写入响应
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
