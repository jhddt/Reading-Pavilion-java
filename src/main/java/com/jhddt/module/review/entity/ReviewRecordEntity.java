package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作文评审记录实体（对应表：review_record）
 */
@Schema(description = "作文评审记录")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("review_record")
public class ReviewRecordEntity {

    /**
     * 评审记录ID
     */
    @Schema(description = "评审记录ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 作文ID
     */
    @Schema(description = "作文ID", required = true)
    @TableField("essay_id")
    private Long essayId;

    /**
     * 异步任务ID
     */
    @Schema(description = "异步任务ID")
    @TableField("task_id")
    private Long taskId;

    /**
     * 评审提示词（最大长度：255）
     * <p>
     * 存储用于本次评审的完整提示词，包含作文内容、评分维度要求等信息。
     * 用于追溯不同提示词的批改情况，支持后续自定义评分规则功能。
     * </p>
     */
    @Schema(description = "评审提示词", example = "请对以下作文进行详细批改...")
    @TableField("rule_version")
    private String ruleVersion;

    /**
     * 评审者类型：0-AI，1-教师
     */
    @Schema(description = "评审者类型：0-AI，1-教师", example = "0")
    @TableField("reviewer_type")
    private Integer reviewerType;

    /**
     * 教师ID（当 reviewer_type=1 时有效）
     */
    @Schema(description = "教师ID")
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * AI 模型版本
     */
    @Schema(description = "AI模型版本", example = "deepseek-chat")
    @TableField("model_version")
    private String modelVersion;

    /**
     * 开始评审时间
     */
    @Schema(description = "开始评审时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束评审时间
     */
    @Schema(description = "结束评审时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 总分
     */
    @Schema(description = "总分", example = "85.50")
    @TableField("total_score")
    private BigDecimal totalScore;

    /**
     * 评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT
     */
    @Schema(description = "评审状态：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT", example = "0")
    @TableField("status")
    private Integer status;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数", example = "0")
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}

