package com.jhddt.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户信息")
public class UserProfileResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "zck")
    private String userName;

    @Schema(description = "用户角色：1-学生，2-教师，3-管理员", example = "1")
    private Integer role;

    @Schema(description = "头像地址", example = "https://example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "头像预览地址", example = "https://example.com/avatar-preview.png")
    private String avatarPreviewUrl;

    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "头像更新时间")
    private LocalDateTime avatarUpdateTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
