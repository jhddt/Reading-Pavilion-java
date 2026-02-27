package com.jhddt.module.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jhddt.module.file.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
}
