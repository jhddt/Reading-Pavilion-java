package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本纠错结果 DTO（用于测试接口返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文本纠错结果")
public class TextCorrectionDTO {

    @Schema(description = "原始文本片段")
    private String originalText;

    @Schema(description = "修改后文本片段")
    private String correctedText;

    @Schema(description = "错误起始位置（字符索引）")
    private Integer startOffset;

    @Schema(description = "错误结束位置（字符索引）")
    private Integer endOffset;

    @Schema(description = "错误类型（grammar/spelling/word_choice等）")
    private String errorType;

    @Schema(description = "修改建议说明")
    private String suggestion;
}
