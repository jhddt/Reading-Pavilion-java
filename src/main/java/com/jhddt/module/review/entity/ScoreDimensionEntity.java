package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分维度配置实体（对应表：score_dimension）
 */
@Schema(description = "评分维度配置")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("score_dimension")
public class ScoreDimensionEntity {

    /**
     * 评分维度ID
     */
    @Schema(description = "评分维度ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "dimension_id", type = IdType.AUTO)
    private Long dimensionId;

    /**
     * 维度名称
     */
    @Schema(description = "维度名称", example = "内容评价")
    @TableField("dimension_name")
    private String dimensionName;

    /**
     * 权重
     */
    @Schema(description = "权重", example = "30.00")
    @TableField("weight")
    private BigDecimal weight;

    /**
     * 满分值
     */
    @Schema(description = "满分值", example = "30.00")
    @TableField("max_score")
    private BigDecimal maxScore;

    /**
     * 状态：1-启用，0-禁用
     */
    @Schema(description = "状态：1-启用，0-禁用", example = "1")
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}

