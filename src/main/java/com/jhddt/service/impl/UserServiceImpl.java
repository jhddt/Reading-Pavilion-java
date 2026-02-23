package com.jhddt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.domain.entity.UserEntity;
import com.jhddt.mapper.UserMapper;
import com.jhddt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean save(UserEntity entity) {
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        return super.save(entity);
    }

    @Override
    public boolean updateById(UserEntity entity) {
        if (entity.getPassword() != null && !entity.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        return super.updateById(entity);
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
}