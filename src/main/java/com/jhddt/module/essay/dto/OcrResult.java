package com.jhddt.module.essay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * OCR 识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    
    /**
     * 原始文本
     */
    private String text;
    
    /**
     * 格式化文本
     */
    @JsonProperty("formatted_text")
    private String formattedText;
    
    /**
     * 识别准确率（百分比）
     */
    private Double accuracy;
    
    /**
     * 准确率类型
     */
    @JsonProperty("accuracy_type")
    private String accuracyType;
    
    /**
     * 准确率详情
     */
    @JsonProperty("accuracy_detail")
    private Map<String, Object> accuracyDetail;
    
    /**
     * 旋转角度
     */
    @JsonProperty("rotation_angle")
    private Integer rotationAngle;
    
    /**
     * OCR 识别对比图（base64）
     */
    @JsonProperty("result_image")
    private String resultImage;
    
    /**
     * 文本块列表（包含位置信息）
     */
    @JsonProperty("text_blocks")
    private List<TextBlock> textBlocks;
    
    /**
     * 图片信息
     */
    @JsonProperty("image_info")
    private ImageInfo imageInfo;
    
    /**
     * 文本块
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextBlock {
        /**
         * 文本块ID
         */
        private Integer id;
        
        /**
         * 识别的文字
         */
        private String text;
        
        /**
         * 识别置信度
         */
        private Double confidence;
        
        /**
         * 文字位置信息
         */
        private BoundingBox box;
    }
    
    /**
     * 位置框信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        /**
         * 四个顶点坐标 [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]
         */
        private List<List<Double>> points;
        
        /**
         * 最小 x 坐标
         */
        @JsonProperty("x_min")
        private Integer xMin;
        
        /**
         * 最小 y 坐标
         */
        @JsonProperty("y_min")
        private Integer yMin;
        
        /**
         * 最大 x 坐标
         */
        @JsonProperty("x_max")
        private Integer xMax;
        
        /**
         * 最大 y 坐标
         */
        @JsonProperty("y_max")
        private Integer yMax;
        
        /**
         * 宽度
         */
        private Integer width;
        
        /**
         * 高度
         */
        private Integer height;
        
        /**
         * 中心点 x 坐标
         */
        @JsonProperty("center_x")
        private Integer centerX;
        
        /**
         * 中心点 y 坐标
         */
        @JsonProperty("center_y")
        private Integer centerY;
    }
    
    /**
     * 图片信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        /**
         * 图片宽度
         */
        private Integer width;
        
        /**
         * 图片高度
         */
        private Integer height;
        
        /**
         * 文本块总数
         */
        @JsonProperty("total_text_blocks")
        private Integer totalTextBlocks;
    }
}
