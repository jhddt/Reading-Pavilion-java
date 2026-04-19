package com.jhddt.module.review.vo;

import com.jhddt.module.review.dto.TextCorrectionDTO;
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
     * 作文标题（方便前端直接展示是哪一篇作文）
     */
    @Schema(description = "作文标题")
    private String essayTitle;

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
     * 本次批改使用的细则ID
     */
    @Schema(description = "本次批改使用的细则ID")
    private Long ruleId;

    /**
     * 本次批改使用的细则名称
     */
    @Schema(description = "本次批改使用的细则名称")
    private String ruleName;

    /**
     * 批改类型
     */
    @Schema(description = "批改类型")
    private String reviewType;

    /**
     * 适用学段
     */
    @Schema(description = "适用学段")
    private String gradeLevel;

    /**
     * 题目要求
     */
    @Schema(description = "题目要求")
    private String topicRequirement;

    /**
     * 润色等级
     */
    @Schema(description = "润色等级")
    private String beautifyLevel;

    /**
     * 自定义要求
     */
    @Schema(description = "自定义要求")
    private String customRequirement;

    /**
     * 扣分细则
     */
    @Schema(description = "扣分细则")
    private String deductionDetail;

    /**
     * 该作文的第几次批改
     */
    @Schema(description = "该作文的第几次批改")
    private Integer reviewVersion;

    /**
     * 是否为当前最新批改
     */
    @Schema(description = "是否为当前最新批改")
    private Boolean latestVersion;

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

    /**
     * 文本纠错列表
     */
    @Schema(description = "文本纠错列表")
    private List<TextCorrectionDTO> textCorrections;
}
