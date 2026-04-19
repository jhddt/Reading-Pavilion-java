package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结构化评分结果")
public class StructuredReviewPayloadDTO {

    @Schema(description = "评分项列表")
    private List<StructuredScoreItemDTO> items;

    @Schema(description = "总分")
    private BigDecimal totalScore;
}
