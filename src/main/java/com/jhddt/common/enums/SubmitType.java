package com.jhddt.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作文提交方式枚举
 */
@Getter
public enum SubmitType {

    IMAGE(0, "图片"),
    DOCUMENT(1, "文档"),
    TEXT(2, "文本");

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

    SubmitType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Integer, SubmitType> CACHE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(SubmitType::getCode, e -> e));

    /**
     * JSON反序列化支持
     */
    @JsonCreator
    public static SubmitType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        SubmitType type = CACHE.get(code);
        if (type == null) {
            throw new IllegalArgumentException("Invalid SubmitType code: " + code);
        }
        return type;
    }
}
