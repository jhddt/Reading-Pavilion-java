package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "批量批改响应")
public class BatchReviewResponse {
    private Long batchTaskId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private List<BatchReviewItemResult> items;
}
