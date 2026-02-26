package com.jhddt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.common.util.JwtUtil;
import com.jhddt.config.security.UserDetailsServiceImpl;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;
import com.jhddt.module.user.mapper.UserMapper;
import com.jhddt.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean save(UserEntity entity) {
        // 保存用户时，对密码进行加密
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        return super.save(entity);
    }

    @Override
    public boolean updateById(UserEntity entity) {
        // 更新用户时，如果传了密码，则重新加密
        if (entity.getPassword() != null && !entity.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        return super.updateById(entity);
    }

    @Override
    public UserEntity getByUserName(String userName) {
        return this.getOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName));
    }

    @Override
    public boolean existsByUserName(String userName) {
        return this.count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName)) > 0;
    }

    @Override
    public boolean existsByUserNameAndIdNot(String userName, Long id) {
        return this.count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName)
                .ne(UserEntity::getId, id)) > 0;
    }

    /**
     * 用户登录（使用 Spring Security 标准认证流程）
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应（包含 Token）
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // 1. 创建认证令牌（包含用户名和密码）
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
                            loginRequest.getPassword()
                    );

            // 2. 调用 AuthenticationManager 进行认证
            // 它会自动调用 UserDetailsService.loadUserByUsername() 加载用户
            // 然后使用 PasswordEncoder.matches() 验证密码
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 3. 认证成功，获取用户名
            String username = authentication.getName();

            // 4. 从数据库获取完整的用户信息（用于返回）
            UserEntity user = userDetailsService.getUserEntityByUsername(username);

            // 5. 生成 JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUserName(), user.getRole());

            // 6. 返回登录响应
            return new LoginResponse(token, user.getId(), user.getUserName(), user.getRole());

        } catch (AuthenticationException e) {
            // 认证失败（用户名不存在或密码错误）
            throw new RuntimeException("用户名或密码错误");
        }
    }
}
