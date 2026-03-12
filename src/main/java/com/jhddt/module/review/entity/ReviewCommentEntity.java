package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 评价与建议实体（对应表：review_comment）
 */
@Schema(description = "评价与建议")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("review_comment")
public class ReviewCommentEntity {

    /**
     * 评论ID
     */
    @Schema(description = "评论ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    /**
     * 所属评审记录ID
     */
    @Schema(description = "所属评审记录ID", required = true)
    @TableField("review_id")
    private Long reviewId;

    /**
     * 评论类型：1-总评，2-建议，3-修改意见
     */
    @Schema(description = "评论类型：1-总评，2-建议，3-修改意见", example = "1")
    @TableField("comment_type")
    private Integer commentType;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    /**
     * 关联文本起始位置（字符索引）
     */
    @Schema(description = "关联文本起始位置")
    @TableField("start_offset")
    private Integer startOffset;

    /**
     * 关联文本结束位置（字符索引）
     */
    @Schema(description = "关联文本结束位置")
    @TableField("end_offset")
    private Integer endOffset;

    /**
     * 关联的原文片段
     */
    @Schema(description = "关联的原文片段")
    @TableField("related_text")
    private String relatedText;

    /**
     * 生成时间
     */
    @Schema(description = "生成时间", accessMode = Schema.AccessMode.READ_ONLY)
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

