package com.jhddt.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批改状态")
public class ReviewStatusVO {

    @Schema(description = "评审记录ID")
    private Long reviewId;

    @Schema(description = "作文ID")
    private Long essayId;

    @Schema(description = "评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT")
    private Integer status;

    @Schema(description = "总分")
    private BigDecimal totalScore;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "状态更新时间")
    private LocalDateTime updateTime;
}
