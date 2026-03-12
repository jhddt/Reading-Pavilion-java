package com.jhddt.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.module.file.entity.FileEntity;

/**
 * 文件服务
 */
public interface FileService extends IService<FileEntity> {
    
    /**
     * 根据作文ID获取文件列表
     */
    java.util.List<FileEntity> getByEssayId(Long essayId, Long userId);
}
