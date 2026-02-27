package com.jhddt.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作文状态枚举
 */
@Getter
public enum EssayStatus {

    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    CORRECTING(2, "批改中"),
    CORRECTED(3, "已批改"),
    ARCHIVED(4, "已归档");

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

    EssayStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Integer, EssayStatus> CACHE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(EssayStatus::getCode, e -> e));

    /**
     * JSON反序列化支持
     */
    @JsonCreator
    public static EssayStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        EssayStatus status = CACHE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Invalid EssayStatus code: " + code);
        }
        return status;
    }
}
