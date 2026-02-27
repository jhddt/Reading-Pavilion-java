package com.jhddt.module.essay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.common.enums.EssayStatus;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.mapper.EssayMapper;
import com.jhddt.module.essay.service.EssayService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 作文 Service 实现类
 */
@Service
public class EssayServiceImpl extends ServiceImpl<EssayMapper, EssayEntity> implements EssayService {

    @Override
    public List<EssayEntity> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<EssayEntity>()
                .eq(EssayEntity::getUserId, userId)
                .orderByDesc(EssayEntity::getCreateTime));
    }

    @Override
    public Page<EssayEntity> pageByUserId(Long userId, Integer page, Integer pageSize, Integer status) {
        // 创建分页对象
        Page<EssayEntity> pageParam = new Page<>(page, pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<EssayEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 必须条件：where user_id = 当前用户
        queryWrapper.eq(EssayEntity::getUserId, userId);
        
        // 可选条件：按状态筛选
        if (status != null) {
            queryWrapper.eq(EssayEntity::getStatus, EssayStatus.values()[status]);
        }
        
        // 排序：按创建时间倒序
        queryWrapper.orderByDesc(EssayEntity::getCreateTime);
        
        // 执行分页查询
        return this.page(pageParam, queryWrapper);
    }

    @Override
    public boolean submitEssay(Long essayId, Long userId) {
        // 1. 查询作文
        EssayEntity essay = this.getById(essayId);
        if (essay == null) {
            throw new RuntimeException("作文不存在");
        }

        // 2. 校验归属
        if (!essay.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此作文");
        }

        // 3. 校验状态（只有草稿可以提交）
        if (!EssayStatus.DRAFT.equals(essay.getStatus())) {
            throw new RuntimeException("只有草稿状态的作文可以提交");
        }

        // 4. 修改状态为已提交
        essay.setStatus(EssayStatus.SUBMITTED);
        
        // 5. 更新数据库（update_time 会自动更新）
        return this.updateById(essay);
    }

    @Override
    public boolean withdrawEssay(Long essayId, Long userId) {
        // 1. 查询作文
        EssayEntity essay = this.getById(essayId);
        if (essay == null) {
            throw new RuntimeException("作文不存在");
        }

        // 2. 校验归属
        if (!essay.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此作文");
        }

        // 3. 校验状态（只有已提交可以撤回）
        if (!EssayStatus.SUBMITTED.equals(essay.getStatus())) {
            throw new RuntimeException("只有已提交状态的作文可以撤回");
        }

        // 4. 修改状态为草稿
        essay.setStatus(EssayStatus.DRAFT);
        
        // 5. 更新数据库（update_time 会自动更新）
        return this.updateById(essay);
    }

    @Override
    public boolean checkEssayOwner(Long essayId, Long userId) {
        EssayEntity essay = this.getById(essayId);
        return essay != null && essay.getUserId().equals(userId);
    }
}
