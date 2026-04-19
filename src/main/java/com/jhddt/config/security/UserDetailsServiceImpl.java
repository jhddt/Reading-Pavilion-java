package com.jhddt.config.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jhddt.common.security.RoleConstants;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 用户详情服务实现
 * 负责从数据库加载用户信息供 Spring Security 进行认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户信息
     * Spring Security 会自动调用此方法进行认证
     *
     * @param username 用户名
     * @return UserDetails 对象（包含用户名、密码、权限等）
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername 被调用，用户名: {}", username);
        
        // 1. 从数据库查询用户
        // 注意：MyBatis-Plus 会自动过滤 is_deleted=1 的数据
        // 所以被逻辑删除的用户也会查询不到
        UserEntity userEntity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUserName, username)
        );
        
        log.info("数据库查询结果: {}", userEntity);

        // 2. 用户不存在或被逻辑删除，统一抛出异常
        // 重要：不要暴露"用户不存在"或"用户被删除"等细节
        // 统一提示"用户名或密码错误"，防止枚举攻击
        if (userEntity == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        // 3. 构建权限列表（兼容旧 ROLE_数字 + 新 ROLE_语义名）
        SimpleGrantedAuthority legacyAuthority = new SimpleGrantedAuthority("ROLE_" + userEntity.getRole());
        SimpleGrantedAuthority namedAuthority = new SimpleGrantedAuthority("ROLE_" + RoleConstants.fromRoleCode(userEntity.getRole()));
        log.info("用户权限: {}, {}", legacyAuthority, namedAuthority);

        // 4. 返回 Spring Security 的 User 对象
        // 参数：用户名、密码（加密后的）、权限列表
        UserDetails userDetails = User.builder()
                .username(userEntity.getUserName())
                .password(userEntity.getPassword())  // 已加密的密码
                .authorities(List.of(legacyAuthority, namedAuthority))
                .build();
        
        log.info("返回 UserDetails，用户名: {}", userDetails.getUsername());
        return userDetails;
    }

    /**
     * 根据用户名获取用户实体（用于登录后返回用户信息）
     *
     * @param username 用户名
     * @return 用户实体
     */
    public UserEntity getUserEntityByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUserName, username)
        );
    }
}
