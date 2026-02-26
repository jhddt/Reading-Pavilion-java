package com.jhddt.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jhddt.common.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
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
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String userName;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 用户角色：1-学生，2-教师，3-管理员
     */
    private Integer role;

    /**
     * 用户状态：0-禁用，1-启用
     */
    private UserStatus status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
