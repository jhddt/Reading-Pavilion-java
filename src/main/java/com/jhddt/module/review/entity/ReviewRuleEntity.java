package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 批改细则主表实体
 */
@Schema(description = "批改细则")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("review_rule")
public class ReviewRuleEntity {

    @Schema(description = "批改细则ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    @Schema(description = "细则名称", example = "小学【叙事类】作文批改")
    @TableField("rule_name")
    private String ruleName;

    @Schema(description = "批改类型", example = "通用作文")
    @TableField("review_type")
    private String reviewType;

    @Schema(description = "年级/学段", example = "小学")
    @TableField("grade_level")
    private String gradeLevel;

    @Schema(description = "基础提示词模板")
    @TableField("prompt_template")
    private String promptTemplate;

    @Schema(description = "题干要求")
    @TableField("topic_requirement")
    private String topicRequirement;

    @Schema(description = "原文美化等级", example = "中度")
    @TableField("beautify_level")
    private String beautifyLevel;

    @Schema(description = "自定义批改要求")
    @TableField("custom_requirement")
    private String customRequirement;

    @Schema(description = "扣分细则")
    @TableField("deduction_detail")
    private String deductionDetail;

    @Schema(description = "写作手法要求（JSON格式）")
    @TableField("writing_techniques")
    private String writingTechniques;

    @Schema(description = "状态：1-启用，0-禁用", example = "1")
    @TableField("status")
    private Integer status;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
