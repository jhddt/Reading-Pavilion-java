package com.jhddt.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.service.EssayService;
import com.jhddt.module.file.entity.FileEntity;
import com.jhddt.module.file.mapper.FileMapper;
import com.jhddt.module.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件服务实现
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements FileService {

    private final EssayService essayService;

    @Override
    public List<FileEntity> getByEssayId(Long essayId, Long userId) {
        // 验证作文归属
        EssayEntity essay = essayService.getById(essayId);
        if (essay == null) {
            throw new IllegalArgumentException("作文不存在");
        }
        if (!essay.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问此作文");
        }

        // 查询文件列表
        return list(new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getEssayId, essayId)
                .orderByAsc(FileEntity::getId));
    }
}

