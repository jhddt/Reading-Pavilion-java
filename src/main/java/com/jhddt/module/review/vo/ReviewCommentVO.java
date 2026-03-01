package com.jhddt.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评审评论 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评审评论")
public class ReviewCommentVO {

    /**
     * 评论ID
     */
    @Schema(description = "评论ID")
    private Long commentId;

    /**
     * 评论类型：1-总评，2-建议，3-修改意见
     */
    @Schema(description = "评论类型：1-总评，2-建议，3-修改意见")
    private Integer commentType;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    private String content;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
