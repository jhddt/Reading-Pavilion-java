package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 多维评分结果实体（对应表：review_score）
 */
@Schema(description = "多维评分结果")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("review_score")
public class ReviewScoreEntity {

    /**
     * 评分记录ID
     */
    @Schema(description = "评分记录ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "score_id", type = IdType.AUTO)
    private Long scoreId;

    /**
     * 所属评审记录ID
     */
    @Schema(description = "所属评审记录ID", required = true)
    @TableField("review_id")
    private Long reviewId;

    /**
     * 所属评分维度ID
     */
    @Schema(description = "所属评分维度ID", required = true)
    @TableField("dimension_id")
    private Long dimensionId;

    /**
     * 评分时的权重快照
     */
    @Schema(description = "评分时的权重快照", example = "30.00")
    @TableField("weight_snapshot")
    private BigDecimal weightSnapshot;

    /**
     * 实际得分
     */
    @Schema(description = "实际得分", example = "26.50")
    @TableField("score")
    private BigDecimal score;

    /**
     * 生成时间
     */
    @Schema(description = "生成时间", accessMode = Schema.AccessMode.READ_ONLY)
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

