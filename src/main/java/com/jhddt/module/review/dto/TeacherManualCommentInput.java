package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "教师手动批注输入")
public class TeacherManualCommentInput {
    @Schema(description = "批注类型：2-改进建议，3-修改意见", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer commentType;

    @Schema(description = "批注内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "选中文本起始偏移", example = "120")
    private Integer startOffset;

    @Schema(description = "选中文本结束偏移", example = "136")
    private Integer endOffset;

    @Schema(description = "选中的原文片段")
    private String relatedText;
}
