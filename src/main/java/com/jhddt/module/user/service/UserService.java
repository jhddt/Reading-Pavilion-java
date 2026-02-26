package com.jhddt.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;

public interface UserService extends IService<UserEntity> {

    /**
     * 根据用户名查询用户
     */
    UserEntity getByUserName(String userName);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUserName(String userName);

    /**
     * 检查用户名是否被其他用户使用（排除指定ID）
     */
    boolean existsByUserNameAndIdNot(String userName, Long id);

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应（包含 Token）
     */
    LoginResponse login(LoginRequest loginRequest);
}