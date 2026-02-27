package com.jhddt.module.essay.service;

import com.jhddt.module.essay.dto.OcrResult;

/**
 * OCR 识别服务
 */
public interface OcrService {

    /**
     * 识别图片中的文字
     * @param imagePath 图片路径
     * @return OCR 识别结果（包含文本和准确率）
     */
    OcrResult recognizeText(String imagePath);
}
