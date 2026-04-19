package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批改细则快照")
public class ReviewRuleSnapshotDTO {

    @Schema(description = "细则ID")
    private Long ruleId;

    @Schema(description = "细则名称")
    private String ruleName;

    @Schema(description = "批改类型")
    private String reviewType;

    @Schema(description = "学段")
    private String gradeLevel;

    @Schema(description = "题目要求")
    private String topicRequirement;

    @Schema(description = "润色等级")
    private String beautifyLevel;

    @Schema(description = "自定义要求")
    private String customRequirement;

    @Schema(description = "扣分细则")
    private String deductionDetail;

    @Schema(description = "基础提示模板")
    private String promptTemplate;

    @Schema(description = "快照说明")
    private String snapshotLabel;
}
