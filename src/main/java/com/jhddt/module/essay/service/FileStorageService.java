package com.jhddt.module.essay.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务
 */
public interface FileStorageService {

    /**
     * 存储图片文件
     */
    String storeImage(MultipartFile file, Long userId);

    /**
     * 存储文档文件
     */
    String storeDocument(MultipartFile file, Long userId);

    /**
     * 存储 base64 图片
     * @param base64Data base64 编码的图片数据
     * @param userId 用户ID
     * @param filename 文件名
     * @return 存储路径
     */
    String storeBase64Image(String base64Data, Long userId, String filename);

    /**
     * 删除文件
     */
    void deleteFile(String filePath);
    
    /**
     * 下载文件
     * @param filePath 文件路径
     * @return 文件输入流
     */
    InputStream downloadFile(String filePath);
    
    /**
     * 获取文件访问 URL
     * @param filePath 文件路径
     * @param expirySeconds 过期时间（秒）
     * @return 预签名 URL
     */
    String getFileUrl(String filePath, Integer expirySeconds);
}
