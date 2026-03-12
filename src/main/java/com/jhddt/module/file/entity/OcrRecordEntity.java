package com.jhddt.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCR识别记录实体类
 */
@Schema(description = "OCR识别记录")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("ocr_record")
public class OcrRecordEntity {

    @Schema(description = "OCR记录ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "ocr_id", type = IdType.AUTO)
    private Long ocrId;

    @Schema(description = "对应作文ID")
    @TableField("essay_id")
    private Long essayId;

    @Schema(description = "来源文件ID")
    @TableField("file_id")
    private Long fileId;

    @Schema(description = "OCR版本号", example = "1")
    @TableField("version")
    private Integer version;

    @Schema(description = "是否当前最新版本", example = "1")
    @TableField("is_latest")
    private Integer isLatest;

    @Schema(description = "识别文本")
    @TableField("ocr_text")
    private String ocrText;

    @Schema(description = "OCR识别对比图路径", example = "essay/ocr/result_xxx.jpg")
    @TableField("result_image_path")
    private String resultImagePath;

    @Schema(description = "识别准确率", example = "95.5")
    @TableField("accuracy")
    private Double accuracy;

    @Schema(description = "OCR引擎", example = "PaddleOCR")
    @TableField("engine")
    private String engine;

    @Schema(description = "图片宽度")
    @TableField("image_width")
    private Integer imageWidth;

    @Schema(description = "图片高度")
    @TableField("image_height")
    private Integer imageHeight;

    @Schema(description = "文本块总数")
    @TableField("total_text_blocks")
    private Integer totalTextBlocks;

    @Schema(description = "识别时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
