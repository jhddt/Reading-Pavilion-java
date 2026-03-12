package com.jhddt.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批改记录列表 VO（包含作文信息）
 */
@Schema(description = "批改记录列表VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecordVO {

    @Schema(description = "评审记录ID")
    private Long reviewId;

    @Schema(description = "作文ID")
    private Long essayId;

    @Schema(description = "异步任务ID")
    private Long taskId;

    @Schema(description = "评审提示词")
    private String ruleVersion;

    @Schema(description = "评审者类型：0-AI，1-教师")
    private Integer reviewerType;

    @Schema(description = "教师ID")
    private Long reviewerId;

    @Schema(description = "AI模型版本")
    private String modelVersion;

    @Schema(description = "开始评审时间")
    private LocalDateTime startTime;

    @Schema(description = "结束评审时间")
    private LocalDateTime endTime;

    @Schema(description = "总分")
    private BigDecimal totalScore;

    @Schema(description = "评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT")
    private Integer status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "作文标题")
    private String essayTitle;

    @Schema(description = "作文提交类型：0-图片，1-文档，2-文本")
    private Integer submitType;
}
