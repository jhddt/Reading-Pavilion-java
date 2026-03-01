package com.jhddt.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评审记录详情 VO（包含评分和评论）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评审记录详情")
public class ReviewRecordDetailVO {

    /**
     * 评审记录ID
     */
    @Schema(description = "评审记录ID")
    private Long reviewId;

    /**
     * 作文ID
     */
    @Schema(description = "作文ID")
    private Long essayId;

    /**
     * 评审者类型：0-AI，1-教师
     */
    @Schema(description = "评审者类型：0-AI，1-教师")
    private Integer reviewerType;

    /**
     * 教师ID（当 reviewer_type=1 时有效）
     */
    @Schema(description = "教师ID")
    private Long reviewerId;

    /**
     * AI 模型版本
     */
    @Schema(description = "AI模型版本")
    private String modelVersion;

    /**
     * 开始评审时间
     */
    @Schema(description = "开始评审时间")
    private LocalDateTime startTime;

    /**
     * 结束评审时间
     */
    @Schema(description = "结束评审时间")
    private LocalDateTime endTime;

    /**
     * 总分
     */
    @Schema(description = "总分")
    private BigDecimal totalScore;

    /**
     * 评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT
     */
    @Schema(description = "评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT")
    private Integer status;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String errorMsg;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 各维度得分列表
     */
    @Schema(description = "各维度得分列表")
    private List<ReviewScoreVO> scores;

    /**
     * 评论列表
     */
    @Schema(description = "评论列表")
    private List<ReviewCommentVO> comments;
}
