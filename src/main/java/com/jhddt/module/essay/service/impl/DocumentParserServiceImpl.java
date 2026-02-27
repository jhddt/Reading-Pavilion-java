package com.jhddt.module.essay.service.impl;

import com.jhddt.config.minio.MinioConfig;
import com.jhddt.module.essay.service.DocumentParserService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档解析服务实现
 * 支持格式：.docx, .doc, .pdf, .txt
 */
@Service
@RequiredArgsConstructor
public class DocumentParserServiceImpl implements DocumentParserService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String parseDocument(String documentPath) {
        try {
            // 从 MinIO 下载文档
            InputStream documentStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(documentPath)
                            .build()
            );

            // 根据文件扩展名选择解析器
            String extension = getFileExtension(documentPath);
            
            String content;
            switch (extension.toLowerCase()) {
                case "docx":
                    content = parseDocx(documentStream);
                    break;
                case "doc":
                    content = parseDoc(documentStream);
                    break;
                case "pdf":
                    content = parsePdf(documentStream);
                    break;
                case "txt":
                    content = parseTxt(documentStream);
                    break;
                default:
                    throw new RuntimeException("不支持的文档格式: " + extension + "，仅支持 .docx, .doc, .pdf, .txt");
            }
            
            documentStream.close();
            return content;
            
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 DOCX 文档（Word 2007+）
     */
    private String parseDocx(InputStream inputStream) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder content = new StringBuilder();
        
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText().trim();
            if (!text.isEmpty()) {
                content.append(text).append("\n");
            }
        }
        
        document.close();
        return content.toString().trim();
    }

    /**
     * 解析 DOC 文档（Word 97-2003）
     */
    private String parseDoc(InputStream inputStream) throws Exception {
        HWPFDocument document = new HWPFDocument(inputStream);
        WordExtractor extractor = new WordExtractor(document);
        
        String[] paragraphs = extractor.getParagraphText();
        StringBuilder content = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            String text = paragraph.trim();
            if (!text.isEmpty()) {
                content.append(text).append("\n");
            }
        }
        
        extractor.close();
        document.close();
        return content.toString().trim();
    }

    /**
     * 解析 PDF 文档
     */
    private String parsePdf(InputStream inputStream) throws Exception {
        // PDFBox 3.x 使用 Loader.loadPDF()
        PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        
        String content = stripper.getText(document);
        document.close();
        
        return content.trim();
    }

    /**
     * 解析 TXT 文本文件
     */
    private String parseTxt(InputStream inputStream) throws Exception {
        StringBuilder content = new StringBuilder();
        
        // 尝试 UTF-8 编码读取
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String text = line.trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }
        }
        
        return content.toString().trim();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}
