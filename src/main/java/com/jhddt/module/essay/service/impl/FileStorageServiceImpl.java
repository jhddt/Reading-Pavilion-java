package com.jhddt.module.essay.service.impl;

import com.jhddt.config.file.FileUploadConfig;
import com.jhddt.config.minio.MinioConfig;
import com.jhddt.module.essay.service.FileStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务实现（MinIO）
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileUploadConfig fileUploadConfig;

    @Override
    public String storeImage(MultipartFile file, Long userId) {
        return storeFile(file, userId, fileUploadConfig.getImagePath());
    }

    @Override
    public String storeDocument(MultipartFile file, Long userId) {
        return storeFile(file, userId, fileUploadConfig.getDocumentPath());
    }

    @Override
    public String storeBase64Image(String base64Data, Long userId, String filename) {
        if (base64Data == null || base64Data.isEmpty()) {
            throw new RuntimeException("Base64 数据不能为空");
        }

        try {
            // 解码 base64
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

            // 生成文件名
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String objectName = "essay/ocr/" + userId + "_" + dateStr + "_" + UUID.randomUUID() + "_" + filename;

            // 上传到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(new java.io.ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Base64 图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream downloadFile(String filePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getFileUrl(String filePath, Integer expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(filePath)
                            .expiry(expirySeconds)
                            .method(io.minio.http.Method.GET)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("生成文件访问链接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 存储文件到 MinIO
     */
    private String storeFile(MultipartFile file, Long userId, String subPath) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        InputStream inputStream = null;
        try {
            // 生成文件名：userId_yyyyMMdd_uuid.ext
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = userId + "_" + dateStr + "_" + UUID.randomUUID() + extension;

            // MinIO 对象路径
            String objectName = subPath + "/" + filename;

            // 获取输入流
            inputStream = file.getInputStream();
            long fileSize = file.getSize();
            
            // 上传到 MinIO，设置合理的分片大小
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, fileSize, 10485760) // 10MB 分片大小
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回访问路径
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage() + "。请检查MinIO服务是否正常运行。");
        } finally {
            // 确保关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
