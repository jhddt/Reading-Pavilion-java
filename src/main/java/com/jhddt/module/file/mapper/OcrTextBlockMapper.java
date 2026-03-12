package com.jhddt.module.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jhddt.module.file.entity.OcrTextBlockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OCR文本块 Mapper
 */
@Mapper
public interface OcrTextBlockMapper extends BaseMapper<OcrTextBlockEntity> {

    /**
     * 根据OCR记录ID查询文本块列表
     */
    List<OcrTextBlockEntity> selectByOcrId(@Param("ocrId") Long ocrId);

    /**
     * 批量插入文本块
     */
    int insertBatch(@Param("list") List<OcrTextBlockEntity> list);
}
