package com.jhddt.module.essay.service;

/**
 * MinIO 服务
 */
public interface MinioService {

    /**
     * 获取文件访问 URL
     * @param objectName 对象名称（文件路径）
     * @return 访问 URL
     */
    String getFileUrl(String objectName);

    /**
     * 获取文件预签名 URL（临时访问链接）
     * @param objectName 对象名称
     * @param expireSeconds 过期时间（秒）
     * @return 预签名 URL
     */
    String getPresignedUrl(String objectName, int expireSeconds);
}
