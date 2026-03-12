package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文本纠错记录（对应表：text_correction）
 */
@Schema(description = "文本纠错记录")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("text_correction")
public class TextCorrectionEntity {

    @Schema(description = "纠错记录ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "correction_id", type = IdType.AUTO)
    private Long correctionId;

    @Schema(description = "所属评审记录ID", required = true)
    @TableField("review_id")
    private Long reviewId;

    @Schema(description = "原始文本片段")
    @TableField("original_text")
    private String originalText;

    @Schema(description = "修改后文本片段")
    @TableField("corrected_text")
    private String correctedText;

    @Schema(description = "纠错版本号")
    @TableField("revision_no")
    private Integer revisionNo;

    @Schema(description = "错误起始位置（字符索引）")
    @TableField("start_offset")
    private Integer startOffset;

    @Schema(description = "错误结束位置（字符索引）")
    @TableField("end_offset")
    private Integer endOffset;

    @Schema(description = "错误类型（grammar/spelling/word_choice等）")
    @TableField("error_type")
    private String errorType;

    @Schema(description = "修改建议说明")
    @TableField("suggestion")
    private String suggestion;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

