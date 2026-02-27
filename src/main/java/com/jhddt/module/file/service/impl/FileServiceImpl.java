package com.jhddt.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.module.file.entity.FileEntity;
import com.jhddt.module.file.mapper.FileMapper;
import com.jhddt.module.file.service.FileService;
import org.springframework.stereotype.Service;

/**
 * 文件服务实现
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements FileService {
}
