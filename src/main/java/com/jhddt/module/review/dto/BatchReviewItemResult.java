package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "批量批改单项结果")
public class BatchReviewItemResult {
    private Long essayId;
    private Long reviewId;
    private Boolean success;
    private String message;
}
