package com.jhddt.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作文评审结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "作文评审结果")
public class ReviewResult {
    
    /**
     * 评审内容（包含评分、评语、修改建议等）
     */
    @Schema(description = "评审内容", example = "这是一篇优秀的作文...")
    private String reviewContent;
    
    /**
     * 总分（如果有）
     */
    @Schema(description = "总分", example = "85.5")
    private String score;
    
    /**
     * 评审时间戳
     */
    @Schema(description = "评审时间戳")
    private Long timestamp;
}
