package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "教师手动批改请求")
public class TeacherManualReviewRequest {
    @Schema(description = "作文ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long essayId;

    @Schema(description = "来源AI评审ID（可选，老师修订AI结果时传）")
    private Long sourceReviewId;

    @Schema(description = "规则快照文本（可选）")
    private String ruleVersion;

    @Schema(description = "总评")
    private String summary;

    @Schema(description = "改进建议")
    private String suggestions;

    @Schema(description = "修改意见")
    private String revisions;

    @Schema(description = "维度得分")
    private List<TeacherScoreInput> scores;

    @Schema(description = "文中批注（建议/修改意见，带位置）")
    private List<TeacherManualCommentInput> annotations;
}
