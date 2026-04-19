package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量批改请求")
public class BatchReviewRequest {

    @Schema(description = "作文ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> essayIds;

    @Schema(description = "批改规则ID（可选）")
    private Long ruleId;
}
