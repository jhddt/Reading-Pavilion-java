package com.jhddt.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatus {

    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    /**
     * 数据库存储值 + JSON输出值
     */
    @EnumValue
    @JsonValue
    private final Integer code;

    /**
     * 展示描述（仅业务层使用）
     */
    private final String description;

    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Integer, UserStatus> CACHE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(UserStatus::getCode, e -> e));

    /**
     * JSON反序列化支持
     */
    @JsonCreator
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        UserStatus status = CACHE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Invalid UserStatus code: " + code);
        }
        return status;
    }
}