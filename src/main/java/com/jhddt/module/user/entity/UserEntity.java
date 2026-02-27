package com.jhddt.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jhddt.common.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Schema(description = "用户实体")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user")
public class UserEntity {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zhangsan", required = true)
    @TableField("username")
    private String userName;

    /**
     * 密码（BCrypt加密）
     */
    @Schema(description = "密码", example = "123456", required = true)
    private String password;

    /**
     * 用户角色：1-学生，2-教师，3-管理员
     */
    @Schema(description = "用户角色：1-学生，2-教师，3-管理员", example = "1")
    private Integer role;

    /**
     * 用户状态：0-禁用，1-启用
     */
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private UserStatus status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
