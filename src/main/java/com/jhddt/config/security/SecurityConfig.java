package com.jhddt.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 配置安全策略、JWT 过滤器、认证管理器等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（因为使用 JWT，不需要 CSRF 保护）
                .csrf(AbstractHttpConfigurer::disable)

                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 允许登录接口匿名访问
                        .requestMatchers("/user/login").permitAll()
                        // 允许注册接口匿名访问
                        .requestMatchers("/user/add").permitAll()
                        // 允许 Swagger 相关路径匿名访问
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )

                // 配置 Session 管理：无状态（不使用 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置认证提供者
                .authenticationProvider(authenticationProvider())

                // 配置异常处理
                .exceptionHandling(exception -> exception
                        // 未认证时的处理（返回 401）
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // 权限不足时的处理（返回 403）
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 添加 JWT 过滤器（在用户名密码认证过滤器之前执行）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置认证提供者
     * 使用 DaoAuthenticationProvider，它会自动调用 UserDetailsService 和 PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // 设置用户详情服务（从数据库加载用户）
        provider.setUserDetailsService(userDetailsService);
        // 设置密码编码器（用于验证密码）
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 暴露 AuthenticationManager Bean
     * 用于在 Service 层手动触发认证
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
