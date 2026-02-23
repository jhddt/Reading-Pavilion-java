package com.jhddt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.domain.entity.UserEntity;

public interface UserService extends IService<UserEntity> {

    boolean existsByUserName(String userName);

    boolean existsByUserNameAndIdNot(String userName, Long id);
}