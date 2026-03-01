package com.jhddt.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 评审得分 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评审得分")
public class ReviewScoreVO {

    /**
     * 评分记录ID
     */
    @Schema(description = "评分记录ID")
    private Long scoreId;

    /**
     * 评分维度ID
     */
    @Schema(description = "评分维度ID")
    private Long dimensionId;

    /**
     * 维度名称
     */
    @Schema(description = "维度名称")
    private String dimensionName;

    /**
     * 权重快照
     */
    @Schema(description = "权重快照")
    private BigDecimal weightSnapshot;

    /**
     * 实际得分
     */
    @Schema(description = "实际得分")
    private BigDecimal score;
}
