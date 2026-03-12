package com.jhddt.module.file.controller;

import com.jhddt.common.result.Result;
import com.jhddt.module.essay.service.FileStorageService;
import com.jhddt.module.file.entity.FileEntity;
import com.jhddt.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件下载", description = "文件下载和访问链接生成接口")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileStorageService fileStorageService;
    private final FileService fileService;

    @Operation(
            summary = "下载文件",
            description = "根据文件ID下载原始上传的文件（图片或文档），只能下载自己上传的文件。" +
                    "返回文件流，浏览器会自动下载文件"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "下载成功，返回文件流",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(responseCode = "404", description = "文件不存在"),
            @ApiResponse(responseCode = "403", description = "无权下载此文件")
    })
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "文件ID", required = true, example = "1")
            @PathVariable Long fileId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        // 查询文件记录
        FileEntity fileEntity = fileService.getById(fileId);
        if (fileEntity == null) {
            throw new RuntimeException("文件不存在");
        }

        // 校验权限
        if (!fileEntity.getUserId().equals(userId)) {
            throw new RuntimeException("无权下载此文件");
        }

        // 下载文件
        InputStream inputStream = fileStorageService.downloadFile(fileEntity.getFilePath());

        // 设置响应头
        String encodedFilename = URLEncoder.encode(fileEntity.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @Operation(
            summary = "获取文件访问链接",
            description = "生成文件的临时访问链接（默认有效期7天），用于预览或分享。" +
                    "返回的 URL 可以直接在浏览器中访问，无需登录"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "生成成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "生成成功",
                                      "data": "http://192.168.41.128:9000/essayandfile/essay/images/1_20260227_abc123.jpg?X-Amz-Algorithm=..."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "文件不存在"),
            @ApiResponse(responseCode = "403", description = "无权访问此文件")
    })
    @GetMapping("/url/{fileId}")
    public Result<String> getFileUrl(
            @Parameter(description = "文件ID", required = true, example = "1")
            @PathVariable Long fileId,
            @Parameter(description = "链接有效期（秒），默认7天", example = "604800")
            @RequestParam(defaultValue = "604800") Integer expirySeconds,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        // 查询文件记录
        FileEntity fileEntity = fileService.getById(fileId);
        if (fileEntity == null) {
            return Result.error("文件不存在");
        }

        // 校验权限
        if (!fileEntity.getUserId().equals(userId)) {
            return Result.error("无权访问此文件");
        }

        // 生成访问链接
        String url = fileStorageService.getFileUrl(fileEntity.getFilePath(), expirySeconds);

        return Result.success("生成成功", url);
    }

    @Operation(
            summary = "通过文件路径获取访问链接",
            description = "根据文件路径生成临时访问链接（默认有效期7天），用于预览图片"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "500", description = "文件路径无效")
    })
    @GetMapping("/url/path")
    public Result<String> getFileUrlByPath(
            @Parameter(description = "文件路径", required = true, example = "essay/images/1_20260227_abc123.jpg")
            @RequestParam String filePath,
            @Parameter(description = "链接有效期（秒），默认7天", example = "604800")
            @RequestParam(defaultValue = "604800") Integer expirySeconds,
            Authentication authentication) {

        if (filePath == null || filePath.isEmpty()) {
            return Result.error("文件路径不能为空");
        }

        try {
            // 生成访问链接
            String url = fileStorageService.getFileUrl(filePath, expirySeconds);
            return Result.success("生成成功", url);
        } catch (Exception e) {
            return Result.error("生成链接失败: " + e.getMessage());
        }
    }

    @Operation(
            summary = "获取作文的所有文件",
            description = "根据作文ID获取该作文的所有上传文件列表"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/essay/{essayId}")
    public Result<java.util.List<FileEntity>> getFilesByEssayId(
            @Parameter(description = "作文ID", required = true, example = "1")
            @PathVariable Long essayId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        try {
            java.util.List<FileEntity> files = fileService.getByEssayId(essayId, userId);
            return Result.success(files);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
