package com.jhddt.config.security;

import com.jhddt.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 拦截每个请求，验证 JWT Token 并设置认证信息
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头中获取 Token
        String header = request.getHeader(jwtProperties.getHeaderName());

        // 2. 如果没有 Token 或格式不正确，直接放行（由 Spring Security 判断是否需要认证）
        if (header == null || !header.startsWith(jwtProperties.getTokenPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token（去掉 "Bearer " 前缀）
        String token = header.substring(jwtProperties.getTokenPrefix().length());

        try {
            // 4. 验证 Token 是否有效
            if (jwtUtil.validateToken(token)) {
                // 5. 从 Token 中提取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String userName = jwtUtil.getUserNameFromToken(token);
                Integer role = jwtUtil.getRoleFromToken(token);

                // 6. 创建认证对象（包含用户信息和权限）
                // 这里简单地将角色作为权限，实际项目可以更复杂
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                    // 主体：用户ID
                                null,                                      // 凭证：不需要密码
                                Collections.singletonList(authority)       // 权限列表
                        );

                // 7. 将认证信息设置到 Spring Security 上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token 解析失败，清空认证信息
            SecurityContextHolder.clearContext();
        }

        // 8. 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}
