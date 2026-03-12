package com.jhddt.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.module.file.entity.OcrRecordEntity;

/**
 * OCR记录服务
 */
public interface OcrRecordService extends IService<OcrRecordEntity> {
    
    /**
     * 获取作文的最新OCR记录
     */
    OcrRecordEntity getLatestByEssayId(Long essayId);
    
    /**
     * 根据文件ID获取OCR记录
     */
    OcrRecordEntity getByFileId(Long fileId);
}
