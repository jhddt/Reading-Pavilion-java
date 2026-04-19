package com.jhddt.module.review.cotroller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.common.audit.AuditAction;
import com.jhddt.common.result.Result;
import com.jhddt.common.security.CurrentUser;
import com.jhddt.module.review.dto.BatchReviewRequest;
import com.jhddt.module.review.dto.BatchReviewResponse;
import com.jhddt.module.review.dto.TeacherManualReviewRequest;
import com.jhddt.module.review.entity.ReviewRuleEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.service.ReviewService;
import com.jhddt.module.review.vo.ReviewRecordDetailVO;
import com.jhddt.module.review.vo.ReviewRecordVO;
import com.jhddt.module.review.vo.ReviewStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Review 模块唯一 Controller（按功能聚合）
 * <p>
 * - 作文评审（AI）+落库
 * - 评审记录查询（详情/列表/分页）
 * - 评分维度配置管理（CRUD）
 * </p>
 */
@Tag(name = "作文评审", description = "使用 DeepSeek API 进行作文评审")
@Slf4j
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    // =========================
    // 评审相关
    // =========================

    @Operation(
            summary = "评审作文",
            description = "使用 DeepSeek API 对数据库中的作文进行评审，评审结果会保存到数据库。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "评审成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReviewRecordDetailVO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "评审成功",
                                      "data": {
                                        "reviewId": 1,
                                        "essayId": 100,
                                        "status": 2
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/essay/{id}", produces = "application/json")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @AuditAction(value = "REVIEW_ESSAY_AI", targetType = "essay")
    public Result<ReviewRecordDetailVO> reviewEssay(
            @Parameter(description = "作文ID", required = true, example = "100")
            @PathVariable Long id,
            @Parameter(description = "评分细则ID", example = "1")
            @RequestParam(required = false) Long ruleId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            ReviewRecordDetailVO detail = reviewService.reviewEssayAndSave(id, userId, ruleId);
            return Result.success("评审成功", detail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("评审失败: {}", e.getMessage(), e);
            return Result.error("评审失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量批改作文", description = "一次提交多篇作文进入异步AI批改")
    @PostMapping(value = "/essay/batch", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @AuditAction(value = "REVIEW_ESSAY_BATCH_AI", targetType = "essay")
    public Result<BatchReviewResponse> batchReviewEssays(
            @RequestBody BatchReviewRequest request,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            return Result.success("批量提交成功", reviewService.batchReviewAndSave(request.getEssayIds(), userId, request.getRuleId()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("批量批改失败: " + e.getMessage());
        }
    }

    @Operation(summary = "教师手动批改/修订AI结果", description = "教师可手动提交批改结果，也可基于sourceReviewId修订AI批改")
    @PostMapping(value = "/teacher/manual", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @AuditAction(value = "REVIEW_ESSAY_TEACHER_MANUAL", targetType = "review")
    public Result<ReviewRecordDetailVO> teacherManualReview(
            @RequestBody TeacherManualReviewRequest request,
            Authentication authentication) {
        try {
            Long teacherId = currentUser.id(authentication);
            return Result.success("提交成功", reviewService.createTeacherManualReview(teacherId, request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("提交失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询评审记录详情", description = "根据评审记录ID查询详细信息（包含各维度得分和评论）")
    @GetMapping("/record/{reviewId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<ReviewRecordDetailVO> getReviewRecordDetail(
            @Parameter(description = "评审记录ID", required = true, example = "1")
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            ReviewRecordDetailVO detail = reviewService.getReviewDetail(reviewId, userId);
            if (detail == null) {
                return Result.error(404, "评审记录不存在或无权访问");
            }
            return Result.success("查询成功", detail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询作文的所有评审记录", description = "根据作文ID查询该作文的所有评审记录列表（按创建时间倒序）")
    @GetMapping("/essay/{essayId}/records")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<List<ReviewRecordVO>> getEssayReviewRecords(
            @Parameter(description = "作文ID", required = true, example = "100")
            @PathVariable Long essayId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            return Result.success("查询成功", reviewService.listByEssayId(essayId, userId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分页查询评审记录列表", description = "查询当前用户的评审记录，支持按状态和评审者类型筛选，结果按创建时间倒序排列")
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<Page<ReviewRecordVO>> pageReviewRecords(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "评审状态（可选）：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "评审者类型（可选）：0-AI，1-教师")
            @RequestParam(required = false) Integer reviewerType,
            @Parameter(description = "作文ID（可选）", example = "100")
            @RequestParam(required = false) Long essayId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            return Result.success("查询成功", reviewService.pageRecords(page, pageSize, status, reviewerType, essayId, userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // =========================
    // 批改细则配置
    // =========================

    @Operation(summary = "查询批改细则", description = "查询 review_rule 中的批改细则配置（可选：仅查询启用的细则）")
    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<List<ReviewRuleEntity>> listRules(
            @Parameter(description = "是否仅返回启用细则（status=1）", example = "true")
            @RequestParam(required = false) Boolean enabledOnly,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            return Result.success("查询成功", reviewService.listRules(enabledOnly));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询评审状态", description = "优先从 Redis 查询批改状态，适合前端轮询")
    @GetMapping("/status/{reviewId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<ReviewStatusVO> getReviewStatus(
            @Parameter(description = "评审记录ID", required = true, example = "1")
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            ReviewStatusVO status = reviewService.getReviewStatus(reviewId, userId);
            if (status == null) {
                return Result.error(404, "评审记录不存在或无权访问");
            }
            return Result.success("查询成功", status);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增批改细则", description = "新增一条批改细则配置（ruleName 必填）")
    @PostMapping(value = "/rules", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReviewRuleEntity> createRule(
            @RequestBody ReviewRuleEntity rule,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            return Result.success("创建成功", reviewService.createRule(rule));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新批改细则", description = "更新批改细则配置（支持部分字段更新）")
    @PutMapping(value = "/rules/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateRule(
            @Parameter(description = "细则ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ReviewRuleEntity rule,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            boolean ok = reviewService.updateRule(id, rule);
            return ok ? Result.success() : Result.error("更新失败（细则不存在或已删除）");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用批改细则", description = "通过 enabled=true/false 控制 status=1/0")
    @PatchMapping(value = "/rules/{id}/status", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateRuleStatus(
            @Parameter(description = "细则ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "是否启用", required = true, example = "true")
            @RequestParam Boolean enabled,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            boolean ok = reviewService.updateRuleStatus(id, enabled);
            return ok ? Result.success() : Result.error("更新失败（细则不存在或已删除）");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除批改细则", description = "逻辑删除批改细则（is_deleted=1）")
    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRule(
            @Parameter(description = "细则ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            boolean ok = reviewService.deleteRule(id);
            return ok ? Result.success() : Result.error("删除失败（细则不存在或已删除）");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // =========================
    // 评分维度配置相关
    // =========================

    @Operation(summary = "查询评分维度配置", description = "查询 score_dimension 中的评分维度配置（可选：仅查询启用的维度）")
    @GetMapping("/dimensions")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<List<ScoreDimensionEntity>> listDimensions(
            @Parameter(description = "所属批改细则ID（可选）", example = "1")
            @RequestParam(required = false) Long ruleId,
            @Parameter(description = "是否仅返回启用维度（status=1）", example = "true")
            @RequestParam(required = false) Boolean enabledOnly,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            log.info("查询评分维度，userId={}, ruleId={}, enabledOnly={}", userId, ruleId, enabledOnly);
            return Result.success("查询成功", reviewService.listDimensions(ruleId, enabledOnly));
        } catch (Exception e) {
            log.error("查询评分维度失败，ruleId={}, enabledOnly={}, error={}", ruleId, enabledOnly, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增评分维度", description = "新增一条评分维度配置（dimensionName/weight/maxScore 必填）")
    @PostMapping(value = "/dimensions", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(value = "REVIEW_DIMENSION_CREATE", targetType = "dimension")
    public Result<ScoreDimensionEntity> createDimension(
            @RequestBody ScoreDimensionEntity dim,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            log.info("新增评分维度，userId={}, dimensionName={}, ruleId={}", userId, dim.getDimensionName(), dim.getRuleId());
            return Result.success("创建成功", reviewService.createDimension(dim));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("新增评分维度失败，dimensionName={}, error={}", dim.getDimensionName(), e.getMessage(), e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新评分维度", description = "更新评分维度配置（支持部分字段更新）")
    @PutMapping(value = "/dimensions/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(value = "REVIEW_DIMENSION_UPDATE", targetType = "dimension")
    public Result<Void> updateDimension(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ScoreDimensionEntity dim,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            log.info("更新评分维度，userId={}, dimensionId={}, dimensionName={}", userId, id, dim.getDimensionName());
            boolean ok = reviewService.updateDimension(id, dim);
            return ok ? Result.success() : Result.error("更新失败（维度不存在或已删除）");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新评分维度失败，dimensionId={}, error={}", id, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用评分维度", description = "通过 enabled=true/false 控制 status=1/0")
    @PatchMapping(value = "/dimensions/{id}/status", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(value = "REVIEW_DIMENSION_STATUS", targetType = "dimension")
    public Result<Void> updateDimensionStatus(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "是否启用", required = true, example = "true")
            @RequestParam Boolean enabled,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            log.info("更新评分维度状态，userId={}, dimensionId={}, enabled={}", userId, id, enabled);
            boolean ok = reviewService.updateDimensionStatus(id, enabled);
            return ok ? Result.success() : Result.error("更新失败（维度不存在或已删除）");
        } catch (Exception e) {
            log.error("更新评分维度状态失败，dimensionId={}, enabled={}, error={}", id, enabled, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除评分维度", description = "逻辑删除评分维度（is_deleted=1）")
    @DeleteMapping("/dimensions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(value = "REVIEW_DIMENSION_DELETE", targetType = "dimension")
    public Result<Void> deleteDimension(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            log.info("删除评分维度，userId={}, dimensionId={}", userId, id);
            boolean ok = reviewService.deleteDimension(id);
            return ok ? Result.success() : Result.error("删除失败（维度不存在或已删除）");
        } catch (Exception e) {
            log.error("删除评分维度失败，dimensionId={}, error={}", id, e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除评审记录", description = "逻辑删除评审记录（is_deleted=1），仅允许删除自己作文的评审记录")
    @DeleteMapping("/record/{reviewId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<Void> deleteReviewRecord(
            @Parameter(description = "评审记录ID", required = true, example = "1")
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            boolean ok = reviewService.deleteReviewRecord(reviewId, userId);
            return ok ? Result.success() : Result.error("删除失败（评审记录不存在或无权删除）");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // =========================
    // 文本纠错接口
    // =========================

    @Operation(
            summary = "对作文进行文本纠错",
            description = "根据作文ID查询作文内容，调用文本纠错服务并保存纠错结果到数据库"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "纠错成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "纠错成功",
                                      "data": {
                                        "reviewId": 1,
                                        "correctionCount": 45
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/essay/{essayId}/correct", produces = "application/json")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<Map<String, Object>> correctEssay(
            @Parameter(description = "作文ID", required = true, example = "100")
            @PathVariable Long essayId,
            @Parameter(description = "评审记录ID（可选，如果不提供则创建新的评审记录）", example = "1")
            @RequestParam(required = false) Long reviewId,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            Map<String, Object> result = reviewService.correctEssay(essayId, reviewId, userId);
            return Result.success("纠错成功", result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文本纠错失败: {}", e.getMessage(), e);
            return Result.error("文本纠错失败: " + e.getMessage());
        }
    }
}

