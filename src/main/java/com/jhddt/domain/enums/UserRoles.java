package com.jhddt.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRoles {
    STUDENT(1, "学生"),
    TEACHER(2, "教师"),
    ADMIN(3, "管理员");

    @EnumValue
    @JsonValue
    private final Integer code;
    
    private final String description;

    UserRoles(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserRoles fromCode(Integer code) {
        for (UserRoles role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的用户角色代码: " + code);
    }
}
