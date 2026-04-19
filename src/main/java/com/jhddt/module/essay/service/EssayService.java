package com.jhddt.module.essay.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jhddt.module.essay.entity.EssayEntity;

import java.util.List;

/**
 * 作文 Service 接口
 */
public interface EssayService extends IService<EssayEntity> {

    /**
     * 根据用户ID查询作文列表
     *
     * @param userId 用户ID
     * @return 作文列表
     */
    List<EssayEntity> listByUserId(Long userId);

    /**
     * 分页查询用户的作文列表（支持状态筛选）
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页数量
     * @param status   作文状态（可选）
     * @return 分页结果
     */
    Page<EssayEntity> pageByUserId(Long userId, Integer page, Integer pageSize, Integer status);

    /**
     * 分页查询老师/管理员可见作文（默认仅已提交及之后状态）
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 作文状态（可选）
     * @return 分页结果
     */
    Page<EssayEntity> pageForTeacherOrAdmin(Integer page, Integer pageSize, Integer status);

    /**
     * 提交作文（修改状态）
     *
     * @param essayId 作文ID
     * @param userId  用户ID（用于权限校验）
     * @return 是否成功
     */
    boolean submitEssay(Long essayId, Long userId);

    /**
     * 撤回作文（修改状态）
     *
     * @param essayId 作文ID
     * @param userId  用户ID（用于权限校验）
     * @return 是否成功
     */
    boolean withdrawEssay(Long essayId, Long userId);

    /**
     * 校验作文是否属于该用户
     *
     * @param essayId 作文ID
     * @param userId  用户ID
     * @return 是否属于该用户
     */
    boolean checkEssayOwner(Long essayId, Long userId);
}
