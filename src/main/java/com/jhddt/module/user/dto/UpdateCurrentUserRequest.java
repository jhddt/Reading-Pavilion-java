package com.jhddt.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户修改个人信息请求
 */
@Data
@Schema(description = "当前登录用户修改个人信息请求")
public class UpdateCurrentUserRequest {

    @Schema(description = "新用户名", example = "zck_new")
    private String userName;

    @Schema(description = "当前密码，修改密码时必填", example = "123456")
    private String currentPassword;

    @Schema(description = "新密码，不修改密码可不传", example = "654321")
    private String newPassword;
}
