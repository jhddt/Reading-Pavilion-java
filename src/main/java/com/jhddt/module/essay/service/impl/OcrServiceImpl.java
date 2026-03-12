package com.jhddt.module.essay.service.impl;

import com.jhddt.config.minio.MinioConfig;
import com.jhddt.module.essay.dto.OcrResult;
import com.jhddt.module.essay.service.OcrService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;

/**
 * OCR 识别服务实现（调用 FastAPI）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final RestTemplate restTemplate;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${ocr.service.url}")
    private String ocrServiceUrl;

    @Override
    public OcrResult recognizeText(String imagePath) {
        try {
            // 1. 从 MinIO 下载图片
            InputStream imageStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(imagePath)
                            .build()
            );

            // 2. 读取图片字节
            byte[] imageBytes = imageStream.readAllBytes();
            imageStream.close();

            // 3. 构建 multipart/form-data 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 创建文件资源
            ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    // 从路径中提取文件名
                    return imagePath.substring(imagePath.lastIndexOf("/") + 1);
                }
            };
            
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. 调用 FastAPI OCR 服务
            log.info("调用 OCR 服务: {}", ocrServiceUrl + "/ocr");
            
            // 先获取原始响应
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr",
                    requestEntity,
                    String.class
            );
            
            String rawJson = rawResponse.getBody();
            log.info("OCR 响应长度: {} 字符", rawJson != null ? rawJson.length() : 0);
            log.info("OCR 响应前500字符: {}", rawJson != null ? rawJson.substring(0, Math.min(500, rawJson.length())) : "null");
            
            // 检查是否包含关键字段
            if (rawJson != null) {
                log.info("包含 text_blocks: {}", rawJson.contains("text_blocks"));
                log.info("包含 image_info: {}", rawJson.contains("image_info"));
                log.info("包含 textBlocks: {}", rawJson.contains("textBlocks"));
                log.info("包含 imageInfo: {}", rawJson.contains("imageInfo"));
            }
            
            // 再解析为对象
            ResponseEntity<OcrResult> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr",
                    requestEntity,
                    OcrResult.class
            );

            // 5. 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                OcrResult result = response.getBody();
                log.info("OCR 解析结果:");
                log.info("  text: {}", result.getText() != null ? "存在" : "null");
                log.info("  accuracy: {}", result.getAccuracy());
                log.info("  textBlocks: {}", result.getTextBlocks() != null ? result.getTextBlocks().size() + " 个" : "null");
                log.info("  imageInfo: {}", result.getImageInfo() != null ? "存在" : "null");
                if (result.getImageInfo() != null) {
                    log.info("    width: {}", result.getImageInfo().getWidth());
                    log.info("    height: {}", result.getImageInfo().getHeight());
                    log.info("    totalTextBlocks: {}", result.getImageInfo().getTotalTextBlocks());
                }
                return result;
            }

            throw new RuntimeException("OCR 识别失败");
        } catch (Exception e) {
            log.error("OCR 服务调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OCR 服务调用失败: " + e.getMessage(), e);
        }
    }
}
