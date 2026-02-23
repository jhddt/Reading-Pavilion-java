package com.jhddt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jhddt.domain.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user")
public class UserEntity {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String userName;

    private String password;
    private Integer role;
    private UserStatus status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
