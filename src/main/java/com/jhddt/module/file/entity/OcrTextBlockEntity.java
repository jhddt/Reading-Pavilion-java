package com.jhddt.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCR文本块实体类（保存文字位置信息）
 */
@Schema(description = "OCR文本块")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("ocr_text_block")
public class OcrTextBlockEntity {

    @Schema(description = "文本块ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "block_id", type = IdType.AUTO)
    private Long blockId;

    @Schema(description = "对应OCR记录ID")
    @TableField("ocr_id")
    private Long ocrId;

    @Schema(description = "文本块序号")
    @TableField("block_index")
    private Integer blockIndex;

    @Schema(description = "识别的文字")
    @TableField("text")
    private String text;

    @Schema(description = "识别置信度")
    @TableField("confidence")
    private Double confidence;

    @Schema(description = "四个顶点坐标JSON [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]")
    @TableField("points")
    private String points;

    @Schema(description = "最小x坐标")
    @TableField("x_min")
    private Integer xMin;

    @Schema(description = "最小y坐标")
    @TableField("y_min")
    private Integer yMin;

    @Schema(description = "最大x坐标")
    @TableField("x_max")
    private Integer xMax;

    @Schema(description = "最大y坐标")
    @TableField("y_max")
    private Integer yMax;

    @Schema(description = "宽度")
    @TableField("width")
    private Integer width;

    @Schema(description = "高度")
    @TableField("height")
    private Integer height;

    @Schema(description = "中心点x坐标")
    @TableField("center_x")
    private Integer centerX;

    @Schema(description = "中心点y坐标")
    @TableField("center_y")
    private Integer centerY;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
