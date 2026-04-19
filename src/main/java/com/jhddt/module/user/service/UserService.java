package com.jhddt.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.dto.AdminBatchImportResult;
import com.jhddt.module.user.dto.UpdateCurrentUserRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;
import com.jhddt.module.user.vo.UserProfileResponse;

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

    /**
     * 查询当前登录用户信息
     *
     * @param userId 当前登录用户ID
     * @return 当前用户信息
     */
    UserProfileResponse getCurrentUserProfile(Long userId);

    /**
     * 修改当前登录用户个人信息
     *
     * @param userId 当前登录用户ID
     * @param request 修改请求
     */
    void updateCurrentUserProfile(Long userId, UpdateCurrentUserRequest request);

    /**
     * 上传当前登录用户头像
     *
     * @param userId 当前登录用户ID
     * @param file 头像文件
     * @return 更新后的用户信息
     */
    UserProfileResponse uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);

    /**
     * 管理员批量导入用户（CSV）
     *
     * @param file CSV 文件，列：userName,password,role,status
     * @return 导入结果
     */
    AdminBatchImportResult batchImportUsers(org.springframework.web.multipart.MultipartFile file);
}
