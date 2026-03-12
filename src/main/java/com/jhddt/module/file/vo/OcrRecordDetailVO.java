package com.jhddt.module.file.vo;

import com.jhddt.module.file.entity.OcrTextBlockEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OCR识别记录详情VO（包含文本块位置信息）
 */
@Schema(description = "OCR识别记录详情")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrRecordDetailVO {

    @Schema(description = "OCR记录ID")
    private Long ocrId;

    @Schema(description = "对应作文ID")
    private Long essayId;

    @Schema(description = "来源文件ID")
    private Long fileId;

    @Schema(description = "OCR版本号")
    private Integer version;

    @Schema(description = "是否当前最新版本")
    private Integer isLatest;

    @Schema(description = "识别文本")
    private String ocrText;

    @Schema(description = "OCR识别对比图路径")
    private String resultImagePath;

    @Schema(description = "识别准确率")
    private Double accuracy;

    @Schema(description = "OCR引擎")
    private String engine;

    @Schema(description = "图片宽度")
    private Integer imageWidth;

    @Schema(description = "图片高度")
    private Integer imageHeight;

    @Schema(description = "文本块总数")
    private Integer totalTextBlocks;

    @Schema(description = "识别时间")
    private LocalDateTime createTime;

    @Schema(description = "文本块列表（包含位置信息）")
    private List<OcrTextBlockEntity> textBlocks;
}
