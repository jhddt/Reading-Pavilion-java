package com.jhddt.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.module.file.entity.OcrRecordEntity;
import com.jhddt.module.file.mapper.OcrRecordMapper;
import com.jhddt.module.file.service.OcrRecordService;
import org.springframework.stereotype.Service;

/**
 * OCR记录服务实现
 */
@Service
public class OcrRecordServiceImpl extends ServiceImpl<OcrRecordMapper, OcrRecordEntity> implements OcrRecordService {

    @Override
    public OcrRecordEntity getLatestByEssayId(Long essayId) {
        return this.getOne(
                new LambdaQueryWrapper<OcrRecordEntity>()
                        .eq(OcrRecordEntity::getEssayId, essayId)
                        .eq(OcrRecordEntity::getIsLatest, 1)
                        .orderByDesc(OcrRecordEntity::getVersion)
                        .last("LIMIT 1")
        );
    }

    @Override
    public OcrRecordEntity getByFileId(Long fileId) {
        return this.getOne(
                new LambdaQueryWrapper<OcrRecordEntity>()
                        .eq(OcrRecordEntity::getFileId, fileId)
                        .orderByDesc(OcrRecordEntity::getVersion)
                        .last("LIMIT 1")
        );
    }
}
