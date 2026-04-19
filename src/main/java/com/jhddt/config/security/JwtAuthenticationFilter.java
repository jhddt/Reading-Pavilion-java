package com.jhddt.config.security;

import com.jhddt.common.security.RoleConstants;
import com.jhddt.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器
 * 拦截每个请求，验证 JWT Token 并设置认证信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("JWT Filter - 请求路径: {}", requestURI);
        
        // 1. 从请求头中获取 Token
        String header = request.getHeader(jwtProperties.getHeaderName());
        log.debug("JWT Filter - Authorization Header: {}", header);

        // 2. 如果没有 Token 或格式不正确，直接放行（由 Spring Security 判断是否需要认证）
        if (header == null || !header.startsWith(jwtProperties.getTokenPrefix())) {
            log.debug("JWT Filter - 没有 Token 或格式不正确，放行");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token（去掉 "Bearer " 前缀）
        String token = header.substring(jwtProperties.getTokenPrefix().length()).trim();
        log.debug("JWT Filter - 提取的 Token: {}", token.substring(0, Math.min(20, token.length())) + "...");

        try {
            // 4. 验证 Token 是否有效
            if (jwtUtil.validateToken(token)) {
                // 打印token剩余有效期
                printTokenRemainingValidity(token);
                
                // 5. 从 Token 中提取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String userName = jwtUtil.getUserNameFromToken(token);
                Integer role = jwtUtil.getRoleFromToken(token);
                
                log.debug("JWT Filter - Token 验证成功，用户ID: {}, 用户名: {}, 角色: {}", userId, userName, role);

                // 6. 创建认证对象（保留兼容 ROLE_数字，同时补充可读角色名 ROLE_ADMIN 等）
                SimpleGrantedAuthority legacyAuthority = new SimpleGrantedAuthority("ROLE_" + role);
                SimpleGrantedAuthority namedAuthority = new SimpleGrantedAuthority("ROLE_" + RoleConstants.fromRoleCode(role));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                    // 主体：用户ID
                                null,                                      // 凭证：不需要密码
                                List.of(legacyAuthority, namedAuthority)   // 权限列表
                        );

                // 7. 将认证信息设置到 Spring Security 上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT Filter - 认证信息已设置到 SecurityContext");
            } else {
                log.warn("JWT Filter - Token 验证失败");
            }
        } catch (Exception e) {
            // Token 解析失败，清空认证信息
            log.error("JWT Filter - Token 解析异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 8. 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 打印token的剩余有效期
     */
    private void printTokenRemainingValidity(String token) {
        try {
            // 解析token获取过期时间
            Claims claims = jwtUtil.parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            // 计算剩余有效期（毫秒）
            long remainingMillis = expiration.getTime() - now.getTime();

            if (remainingMillis <= 0) {
                log.warn("【Token状态】当前token已过期！过期时间: {}", expiration);
            } else {
                // 转换为更易读的格式
                long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
                long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

                String remainingTime = String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);
                log.info("【Token剩余有效期】{}", remainingTime);
                log.info("【Token过期时间】{}", expiration);
                log.info("【Token剩余毫秒数】{}", remainingMillis);
            }
        } catch (Exception e) {
            log.error("【Token状态】打印token剩余有效期时发生错误: {}", e.getMessage(), e);
        }
    }
}
