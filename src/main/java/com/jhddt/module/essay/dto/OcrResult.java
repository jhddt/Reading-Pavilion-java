package com.jhddt.module.essay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * OCR 识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    
    /**
     * 识别的文本
     */
    private String text;
    
    /**
     * 识别准确率（百分比）
     */
    private BigDecimal accuracy;
    
    /**
     * OCR 识别对比图（base64 或 URL）
     */
    private String resultImage;
}
