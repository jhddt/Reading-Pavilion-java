package com.jhddt.module.file.controller;

import com.jhddt.common.result.Result;
import com.jhddt.module.essay.service.MinioService;
import com.jhddt.module.file.entity.OcrRecordEntity;
import com.jhddt.module.file.entity.OcrTextBlockEntity;
import com.jhddt.module.file.mapper.OcrTextBlockMapper;
import com.jhddt.module.file.service.OcrRecordService;
import com.jhddt.module.file.vo.OcrRecordDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR 记录管理接口
 */
@Tag(name = "OCR管理", description = "OCR识别记录查询接口")
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrRecordService ocrRecordService;
    private final MinioService minioService;
    private final OcrTextBlockMapper ocrTextBlockMapper;

    /**
     * 获取 OCR 记录详情
     */
    @Operation(
            summary = "获取OCR记录详情", 
            description = "根据OCR记录ID查询详细信息，包括识别文本、准确率、对比图路径等"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "OCR记录不存在")
    })
    @GetMapping("/{ocrId}")
    public Result<OcrRecordEntity> getOcrRecord(
            @Parameter(description = "OCR记录ID", required = true, example = "1") 
            @PathVariable Long ocrId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getById(ocrId);
        if (ocrRecord == null) {
            return Result.error("OCR记录不存在");
        }

        return Result.success(ocrRecord);
    }

    /**
     * 获取 OCR 记录详情（包含文本块位置信息）
     */
    @Operation(
            summary = "获取OCR记录详情（含文本块）", 
            description = "根据OCR记录ID查询详细信息，包括文本块位置信息，用于在图片上标注"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "OCR记录不存在")
    })
    @GetMapping("/{ocrId}/detail")
    public Result<OcrRecordDetailVO> getOcrRecordDetail(
            @Parameter(description = "OCR记录ID", required = true, example = "1") 
            @PathVariable Long ocrId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getById(ocrId);
        if (ocrRecord == null) {
            return Result.error("OCR记录不存在");
        }

        // 查询文本块列表
        List<OcrTextBlockEntity> textBlocks = ocrTextBlockMapper.selectByOcrId(ocrId);

        // 构建 VO
        OcrRecordDetailVO vo = OcrRecordDetailVO.builder()
                .ocrId(ocrRecord.getOcrId())
                .essayId(ocrRecord.getEssayId())
                .fileId(ocrRecord.getFileId())
                .version(ocrRecord.getVersion())
                .isLatest(ocrRecord.getIsLatest())
                .ocrText(ocrRecord.getOcrText())
                .resultImagePath(ocrRecord.getResultImagePath())
                .accuracy(ocrRecord.getAccuracy())
                .engine(ocrRecord.getEngine())
                .imageWidth(ocrRecord.getImageWidth())
                .imageHeight(ocrRecord.getImageHeight())
                .totalTextBlocks(ocrRecord.getTotalTextBlocks())
                .createTime(ocrRecord.getCreateTime())
                .textBlocks(textBlocks)
                .build();

        return Result.success(vo);
    }

    /**
     * 获取 OCR 对比图 URL
     */
    @Operation(
            summary = "获取OCR对比图URL", 
            description = "获取OCR识别对比图的临时访问URL（有效期7天），可直接在浏览器或img标签中使用"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "500", description = "OCR记录不存在或没有对比图")
    })
    @GetMapping("/{ocrId}/result-image")
    public Result<Map<String, String>> getResultImage(
            @Parameter(description = "OCR记录ID", required = true, example = "1") 
            @PathVariable Long ocrId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getById(ocrId);
        if (ocrRecord == null) {
            return Result.error("OCR记录不存在");
        }

        if (ocrRecord.getResultImagePath() == null || ocrRecord.getResultImagePath().isEmpty()) {
            return Result.error("该OCR记录没有对比图");
        }

        // 生成访问 URL（7天有效期）
        String imageUrl = minioService.getPresignedUrl(ocrRecord.getResultImagePath(), 7 * 24 * 3600);

        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("imagePath", ocrRecord.getResultImagePath());

        return Result.success(response);
    }

    /**
     * 根据作文ID获取最新的OCR记录
     */
    @Operation(
            summary = "获取作文的OCR记录", 
            description = "根据作文ID获取最新的OCR识别记录（适用于图片上传方式创建的作文）"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "该作文没有OCR记录")
    })
    @GetMapping("/essay/{essayId}")
    public Result<OcrRecordEntity> getOcrByEssayId(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long essayId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getLatestByEssayId(essayId);
        if (ocrRecord == null) {
            return Result.error("该作文没有OCR记录");
        }

        return Result.success(ocrRecord);
    }

    /**
     * 根据文件ID获取OCR记录
     */
    @Operation(
            summary = "根据文件ID获取OCR记录", 
            description = "根据文件ID获取对应的OCR识别记录"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "该文件没有OCR记录")
    })
    @GetMapping("/file/{fileId}")
    public Result<OcrRecordEntity> getOcrByFileId(
            @Parameter(description = "文件ID", required = true, example = "1") 
            @PathVariable Long fileId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getByFileId(fileId);
        if (ocrRecord == null) {
            return Result.error("该文件没有OCR记录");
        }

        return Result.success(ocrRecord);
    }

    /**
     * 获取作文的OCR对比图
     */
    @Operation(
            summary = "获取作文的OCR对比图URL", 
            description = "根据作文ID获取最新OCR识别对比图的临时访问URL（有效期7天），包含准确率等信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "500", description = "该作文没有OCR记录或没有对比图")
    })
    @GetMapping("/essay/{essayId}/result-image")
    public Result<Map<String, String>> getResultImageByEssayId(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long essayId,
            Authentication authentication) {
        
        OcrRecordEntity ocrRecord = ocrRecordService.getLatestByEssayId(essayId);
        if (ocrRecord == null) {
            return Result.error("该作文没有OCR记录");
        }

        if (ocrRecord.getResultImagePath() == null || ocrRecord.getResultImagePath().isEmpty()) {
            return Result.error("该OCR记录没有对比图");
        }

        // 生成访问 URL（7天有效期）
        String imageUrl = minioService.getPresignedUrl(ocrRecord.getResultImagePath(), 7 * 24 * 3600);

        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("imagePath", ocrRecord.getResultImagePath());
        response.put("ocrId", ocrRecord.getOcrId().toString());
        response.put("accuracy", ocrRecord.getAccuracy() != null ? ocrRecord.getAccuracy().toString() : null);

        return Result.success(response);
    }
}
