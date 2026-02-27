package com.jhddt.config.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 文件上传配置
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Data
public class FileUploadConfig {

    /**
     * 文件存储根路径
     */
    private String basePath = "uploads";

    /**
     * 图片存储路径
     */
    private String imagePath = "essay/images";

    /**
     * 文档存储路径
     */
    private String documentPath = "essay/documents";

    /**
     * 允许的图片格式
     */
    private String[] allowedImageTypes = {"jpg", "jpeg", "png"};

    /**
     * 允许的文档格式
     */
    private String[] allowedDocumentTypes = {"doc", "docx", "pdf","txt"};

    /**
     * 最大文件大小（字节）
     */
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
}
