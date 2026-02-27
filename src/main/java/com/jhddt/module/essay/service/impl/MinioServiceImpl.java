package com.jhddt.module.essay.service.impl;

import com.jhddt.config.minio.MinioConfig;
import com.jhddt.module.essay.service.MinioService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务实现
 */
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String getFileUrl(String objectName) {
        // 返回公共访问 URL（需要 bucket 设置为 public）
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
    }

    @Override
    public String getPresignedUrl(String objectName, int expireSeconds) {
        try {
            // 生成预签名 URL（临时访问链接）
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage());
        }
    }
}
