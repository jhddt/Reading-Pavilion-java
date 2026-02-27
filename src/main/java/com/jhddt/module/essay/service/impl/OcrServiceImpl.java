package com.jhddt.module.essay.service.impl;

import com.jhddt.config.minio.MinioConfig;
import com.jhddt.module.essay.dto.OcrResult;
import com.jhddt.module.essay.service.OcrService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

/**
 * OCR 识别服务实现（调用 FastAPI）
 */
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
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr",
                    requestEntity,
                    Map.class
            );

            // 5. 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                
                String text = result.get("text") != null ? result.get("text").toString() : "";
                
                // 解析准确率
                BigDecimal accuracy = null;
                Object accuracyObj = result.get("accuracy");
                if (accuracyObj != null) {
                    if (accuracyObj instanceof Number) {
                        accuracy = BigDecimal.valueOf(((Number) accuracyObj).doubleValue());
                    } else if (accuracyObj instanceof String) {
                        try {
                            accuracy = new BigDecimal(accuracyObj.toString());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                
                // 解析对比图（base64）
                String resultImage = null;
                Object resultImageObj = result.get("result_image");
                if (resultImageObj != null) {
                    resultImage = resultImageObj.toString();
                }
                
                return OcrResult.builder()
                        .text(text)
                        .accuracy(accuracy)
                        .resultImage(resultImage)
                        .build();
            }

            throw new RuntimeException("OCR 识别失败");
        } catch (Exception e) {
            throw new RuntimeException("OCR 服务调用失败: " + e.getMessage(), e);
        }
    }
}
