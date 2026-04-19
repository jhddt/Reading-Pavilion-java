package com.jhddt.common.security;

import com.jhddt.common.enums.UserRoles;

/**
 * 统一角色常量与映射，避免散落的数字角色硬编码。
 */
public final class RoleConstants {

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String ADMIN = "ADMIN";

    private RoleConstants() {
    }

    public static String fromRoleCode(Integer roleCode) {
        if (roleCode == null) {
            return STUDENT;
        }
        return switch (UserRoles.fromCode(roleCode)) {
            case STUDENT -> STUDENT;
            case TEACHER -> TEACHER;
            case ADMIN -> ADMIN;
        };
    }
}
