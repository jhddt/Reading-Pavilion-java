package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "教师评分项输入")
public class TeacherScoreInput {
    @Schema(description = "维度ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dimensionId;

    @Schema(description = "得分", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal score;
}
