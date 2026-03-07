package com.jhddt.module.essay.cotroller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.common.enums.EssayStatus;
import com.jhddt.common.enums.SubmitType;
import com.jhddt.common.result.Result;
import com.jhddt.module.essay.dto.CreateTextEssayRequest;
import com.jhddt.module.essay.dto.OcrResult;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.service.*;
import com.jhddt.module.file.entity.FileEntity;
import com.jhddt.module.file.entity.OcrRecordEntity;
import com.jhddt.module.file.service.FileService;
import com.jhddt.module.file.service.OcrRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "作文管理", description = "作文相关接口")
@RestController
@RequestMapping("/essay")
@RequiredArgsConstructor
public class EssayController {

    private final EssayService essayService;
    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final DocumentParserService documentParserService;
    private final FileService fileService;
    private final OcrRecordService ocrRecordService;

    /**
     * 1. 纯文本创建草稿
     * POST /essay/text
     */
    @Operation(
            summary = "纯文本创建草稿", 
            description = "通过纯文本方式创建作文草稿，适用于用户直接输入或粘贴文本内容的场景"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "创建成功，返回作文ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "创建成功",
                                      "data": 100
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Long> createTextEssay(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "文本作文创建请求",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateTextEssayRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "title": "我的暑假生活",
                                      "content": "这个暑假我过得非常充实..."
                                    }
                                    """)
                    )
            )
            @RequestBody CreateTextEssayRequest request, 
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        EssayEntity essay = EssayEntity.builder()
                .userId(userId)
                .title(request.getTitle())
                .submitType(SubmitType.TEXT)
                .originalContent(request.getContent())
                .finalContent(request.getContent())
                .wordCount(request.getContent().length())
                .status(EssayStatus.DRAFT)
                .build();

        boolean success = essayService.save(essay);
        return success ? Result.success("创建成功", essay.getId()) : Result.error("创建失败");
    }

    /**
     * 2. 图片上传 + OCR 识别创建草稿（支持多张图片合并为一篇作文）
     * POST /essay/image
     */
    @Operation(
            summary = "图片上传创建草稿（支持多张图片）",
            description = "上传一张或多张作文图片（支持 jpg/png/gif/bmp，单张最大5MB），通过 OCR 识别后将多张图片的文字合并为一篇作文草稿。" +
                    "识别结果包含文本内容、准确率和对比图"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "创建成功，返回作文ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "创建成功",
                                      "data": 101
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "创建失败，可能原因：图片格式不支持、文件过大、OCR服务异常等")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
            )
    )
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createImageEssay(
            @Parameter(description = "作文标题", required = true, example = "我的暑假生活")
            @RequestParam String title,
            @Parameter(
                    description = "图片文件列表（支持 jpg/jpeg/png/gif/bmp，单张最大5MB），可上传多张",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                    )
            ) @RequestParam("file") MultipartFile[] files,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        if (files == null || files.length == 0) {
            return Result.error("请至少上传一张图片");
        }

        // 1. 先创建作文记录（不依赖具体哪一张图片）
        EssayEntity essay = EssayEntity.builder()
                .userId(userId)
                .title(title)
                .submitType(SubmitType.IMAGE)
                .status(EssayStatus.DRAFT)
                .build();
        essayService.save(essay);

        StringBuilder mergedText = new StringBuilder();
        String firstSourcePath = null;

        int version = 1;

        // 2. 逐张图片处理：存储文件、OCR 识别、保存 OCR 记录，累加文字
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            // 2.1 存储图片到 MinIO
            String imagePath = fileStorageService.storeImage(file, userId);
            if (firstSourcePath == null) {
                firstSourcePath = imagePath;
            }

            // 2.2 保存文件记录
            FileEntity fileEntity = FileEntity.builder()
                    .userId(userId)
                    .essayId(essay.getId())
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .filePath(imagePath)
                    .fileSize(file.getSize())
                    .build();
            fileService.save(fileEntity);

            // 2.3 OCR 识别
            OcrResult ocrResult = ocrService.recognizeText(imagePath);

            String text = ocrResult.getText() != null ? ocrResult.getText().trim() : "";
            if (!text.isEmpty()) {
                if (mergedText.length() > 0) {
                    mergedText.append("\n");
                }
                mergedText.append(text);
            }

            // 2.4 保存 OCR 对比图（如果有）
            String resultImagePath = null;
            if (ocrResult.getResultImage() != null && !ocrResult.getResultImage().isEmpty()) {
                try {
                    resultImagePath = fileStorageService.storeBase64Image(
                            ocrResult.getResultImage(),
                            userId,
                            "result.jpg"
                    );
                } catch (Exception e) {
                    // 对比图保存失败不影响主流程
                    System.err.println("保存 OCR 对比图失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 2.5 保存 OCR 记录（多张图片时 version 递增）
            OcrRecordEntity ocrRecord = OcrRecordEntity.builder()
                    .essayId(essay.getId())
                    .fileId(fileEntity.getId())
                    .version(version++)
                    .isLatest(1)
                    .ocrText(text)
                    .resultImagePath(resultImagePath)
                    .accuracy(ocrResult.getAccuracy())
                    .engine("PaddleOCR")
                    .build();
            ocrRecordService.save(ocrRecord);
        }

        String finalText = mergedText.toString();
        if (finalText.isEmpty()) {
            return Result.error("OCR 未识别出有效文字，请检查图片是否清晰");
        }

        // 3. 更新作文内容（多张图片的文字合并）
        essay.setSourceFilePath(firstSourcePath);
        essay.setOriginalContent(finalText);
        essay.setFinalContent(finalText);
        essay.setWordCount(finalText.length());
        essayService.updateById(essay);

        return Result.success("创建成功", essay.getId());
    }
    /**
     * 3. 文档上传 + 解析创建草稿
     * POST /essay/document
     */
    @Operation(
            summary = "文档上传创建草稿", 
            description = "上传文档文件（支持 .docx/.doc/.pdf/.txt，最大10MB），自动解析文字内容后创建作文草稿。" +
                    "支持 Word 2007+（.docx）、Word 97-2003（.doc）、PDF 和纯文本格式"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "创建成功，返回作文ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "创建成功",
                                      "data": 102
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "创建失败，可能原因：文档格式不支持、文件过大、文档损坏等")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
            )
    )
    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createDocumentEssay(
            @Parameter(description = "作文标题", required = true, example = "我的暑假生活") 
            @RequestParam String title,
            @Parameter(
                    description = "文档文件（支持 .docx/.doc/.pdf/.txt，最大10MB）", 
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                    )
            ) @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();

        // 1. 存储文档到 MinIO
        String documentPath = fileStorageService.storeDocument(file, userId);

        // 2. 创建作文记录
        EssayEntity essay = EssayEntity.builder()
                .userId(userId)
                .title(title)
                .submitType(SubmitType.DOCUMENT)
                .sourceFilePath(documentPath)
                .status(EssayStatus.DRAFT)
                .build();
        essayService.save(essay);

        // 3. 保存文件记录
        FileEntity fileEntity = FileEntity.builder()
                .userId(userId)
                .essayId(essay.getId())
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .filePath(documentPath)
                .fileSize(file.getSize())
                .build();
        fileService.save(fileEntity);

        // 4. 解析文档（如果失败会抛出异常，触发事务回滚）
        String parsedText = documentParserService.parseDocument(documentPath);

        // 5. 更新作文内容
        essay.setOriginalContent(parsedText);
        essay.setFinalContent(parsedText);
        essay.setWordCount(parsedText.length());
        essayService.updateById(essay);

        return Result.success("创建成功", essay.getId());
    }

    /**
     * 4. 创建草稿（旧接口，保持兼容）
     * POST /essay
     */
    @Operation(
            summary = "创建草稿（通用）", 
            description = "创建一篇新的作文草稿，状态自动设置为草稿。此接口为兼容旧版本保留，建议使用 /text、/image、/document 接口"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    public Result<Long> createDraft(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "作文信息（id、userId、createTime、updateTime 由系统自动生成）",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = EssayEntity.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "title": "我的暑假生活",
                                      "submitType": 2,
                                      "originalContent": "这个暑假我过得非常充实...",
                                      "finalContent": "这个暑假我过得非常充实...",
                                      "wordCount": 500
                                    }
                                    """)
                    )
            )
            @RequestBody EssayEntity essay, 
            Authentication authentication) {
        // 从 token 中获取用户ID
        Long userId = (Long) authentication.getPrincipal();
        
        // 清空 ID，让数据库自动生成
        essay.setId(null);
        // 强制设置用户ID和初始状态
        essay.setUserId(userId);
        essay.setStatus(EssayStatus.DRAFT);
        
        boolean success = essayService.save(essay);
        if (success) {
            return Result.success("创建草稿成功", essay.getId());
        }
        return Result.error("创建草稿失败");
    }

    /**
     * 5. 修改草稿
     * PUT /essay/{id}
     */
    @Operation(
            summary = "修改草稿", 
            description = "修改草稿状态的作文内容，只有草稿状态（status=0）的作文可以修改。已提交的作文需先撤回才能修改"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "500", description = "修改失败，可能原因：作文不存在、无权操作、非草稿状态等")
    })
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public Result<Void> updateDraft(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "作文信息（id、userId、status、createTime、updateTime 无需填写）",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = EssayEntity.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "title": "我的暑假生活（修改版）",
                                      "finalContent": "这个暑假我过得非常充实，修改后的内容...",
                                      "wordCount": 600
                                    }
                                    """)
                    )
            )
            @RequestBody EssayEntity essay,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        // 校验作文是否属于当前用户
        EssayEntity existingEssay = essayService.getById(id);
        if (existingEssay == null) {
            return Result.error("作文不存在");
        }

        // 校验归属
        if (!existingEssay.getUserId().equals(userId)) {
            return Result.error("无权操作此作文");
        }

        // 校验状态：只有草稿状态可以修改
        if (!EssayStatus.DRAFT.equals(existingEssay.getStatus())) {
            return Result.error("只有草稿状态的作文可以修改");
        }

        // 更新数据
        essay.setId(id);
        essay.setUserId(userId);
        essay.setStatus(EssayStatus.DRAFT);
        
        boolean success = essayService.updateById(essay);
        return success ? Result.success() : Result.error("修改草稿失败");
    }

    /**
     * 6. 提交作文（修改状态）
     * PUT /essay/{id}/submit
     */
    @Operation(
            summary = "提交作文", 
            description = "将草稿状态（status=0）的作文提交批改，状态变更为已提交（status=1）。提交后不可修改或删除，需先撤回"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "提交成功"),
            @ApiResponse(responseCode = "500", description = "提交失败，可能原因：作文不存在、无权操作、非草稿状态等")
    })
    @PutMapping(value = "/{id}/submit", consumes = "application/json", produces = "application/json")
    public Result<Void> submitEssay(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            essayService.submitEssay(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 7. 撤回作文（修改状态）
     * PUT /essay/{id}/withdraw
     */
    @Operation(
            summary = "撤回作文", 
            description = "将已提交状态（status=1）的作文撤回为草稿（status=0），撤回后可以修改或删除"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "撤回成功"),
            @ApiResponse(responseCode = "500", description = "撤回失败，可能原因：作文不存在、无权操作、非已提交状态等")
    })
    @PutMapping(value = "/{id}/withdraw", consumes = "application/json", produces = "application/json")
    public Result<Void> withdrawEssay(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            essayService.withdrawEssay(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 8. 查询自己的作文列表（支持分页和状态筛选）
     * GET /essay/list?page=1&pageSize=10&status=1
     */
    @Operation(
            summary = "查询我的作文列表", 
            description = "分页查询当前用户的作文列表，支持按状态筛选。返回结果包含总数、当前页、每页数量等分页信息"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "操作成功",
                                      "data": {
                                        "records": [
                                          {
                                            "id": 100,
                                            "userId": 1,
                                            "title": "我的暑假生活",
                                            "submitType": 2,
                                            "status": 0,
                                            "wordCount": 500,
                                            "createTime": "2026-02-27T10:00:00"
                                          }
                                        ],
                                        "total": 10,
                                        "size": 10,
                                        "current": 1,
                                        "pages": 1
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/list")
    public Result<Page<EssayEntity>> getMyEssayList(
            @Parameter(description = "页码（从1开始）", example = "1") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") 
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "作文状态筛选（可选）：0-草稿，1-已提交，2-批改中，3-已批改，4-已归档", example = "0") 
            @RequestParam(required = false) Integer status,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        
        // 调用 Service 层分页查询
        Page<EssayEntity> result = essayService.pageByUserId(userId, page, pageSize, status);
        
        return Result.success(result);
    }

    /**
     * 9. 查看作文详情
     * GET /essay/{id}
     */
    @Operation(
            summary = "查看作文详情", 
            description = "根据作文ID查询作文详细信息，包括标题、内容、状态、字数、批改结果等。只能查看自己的作文"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = EssayEntity.class))
            ),
            @ApiResponse(responseCode = "500", description = "查询失败，可能原因：作文不存在、无权查看等")
    })
    @GetMapping("/{id}")
    public Result<EssayEntity> getEssayDetail(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();

        // 根据 id 查询
        EssayEntity essay = essayService.getById(id);
        if (essay == null) {
            return Result.error("作文不存在");
        }

        // 校验归属：user_id = 当前用户
        if (!essay.getUserId().equals(userId)) {
            return Result.error("无权查看此作文");
        }

        return Result.success(essay);
    }

    /**
     * 10. 删除草稿（逻辑删除）
     * DELETE /essay/{id}
     */
    @Operation(
            summary = "删除草稿", 
            description = "逻辑删除草稿状态（status=0）的作文，只有草稿状态可以删除。删除后数据仍保留在数据库中，但不再显示"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败，可能原因：作文不存在、无权操作、非草稿状态等")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteDraft(
            @Parameter(description = "作文ID", required = true, example = "100") 
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();

        // 查询作文
        EssayEntity essay = essayService.getById(id);
        if (essay == null) {
            return Result.error("作文不存在");
        }

        // 校验归属
        if (!essay.getUserId().equals(userId)) {
            return Result.error("无权操作此作文");
        }

        // 校验状态：必须是草稿
        if (!EssayStatus.DRAFT.equals(essay.getStatus())) {
            return Result.error("只有草稿状态的作文可以删除");
        }

        // 逻辑删除（MyBatis-Plus 自动处理 is_deleted 字段）
        boolean success = essayService.removeById(id);
        return success ? Result.success() : Result.error("删除草稿失败");
    }
}

