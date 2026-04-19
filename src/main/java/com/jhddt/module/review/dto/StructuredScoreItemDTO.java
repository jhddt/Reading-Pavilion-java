package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结构化评分项")
public class StructuredScoreItemDTO {

    @Schema(description = "评分项名称")
    private String dimensionName;

    @Schema(description = "评分项得分")
    private BigDecimal score;

    @Schema(description = "评分项简评")
    private String comment;
}
