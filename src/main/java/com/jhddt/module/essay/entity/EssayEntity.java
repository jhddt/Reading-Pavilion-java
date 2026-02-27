package com.jhddt.module.essay.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jhddt.common.enums.EssayStatus;
import com.jhddt.common.enums.SubmitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作文实体类
 */
@Schema(description = "作文实体")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("essay")
public class EssayEntity {

    /**
     * 作文ID
     */
    @Schema(description = "作文ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "essay_id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField("user_id")
    private Long userId;

    /**
     * 作文标题
     */
    @Schema(description = "作文标题", example = "我的第一篇作文", required = true)
    private String title;

    /**
     * 提交方式：0-图片，1-文档，2-文本
     */
    @Schema(description = "提交方式：0-图片，1-文档，2-文本", example = "2")
    @TableField("submit_type")
    private SubmitType submitType;

    /**
     * 原始文件路径（图片或文档路径）
     */
    @Schema(description = "原始文件路径", example = "/uploads/essay/123.jpg")
    @TableField("source_file_path")
    private String sourceFilePath;

    /**
     * 首次解析得到的文本内容
     */
    @Schema(description = "首次解析得到的文本内容")
    @TableField("original_content")
    private String originalContent;

    /**
     * 最终用于批改的文本内容
     */
    @Schema(description = "最终用于批改的文本内容", example = "这是作文内容...")
    @TableField("final_content")
    private String finalContent;

    /**
     * 字数统计
     */
    @Schema(description = "字数统计", example = "800")
    @TableField("word_count")
    private Integer wordCount;

    /**
     * 最终总分（冗余字段，提高查询性能）
     */
    @Schema(description = "最终总分", example = "85.5")
    @TableField("final_score")
    private BigDecimal finalScore;

    /**
     * 作文状态：0-草稿，1-已提交，2-批改中，3-已批改，4-已归档
     */
    @Schema(description = "作文状态：0-草稿，1-已提交，2-批改中，3-已批改，4-已归档", example = "0")
    private EssayStatus status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
