package com.jhddt.module.review.cotroller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.common.result.Result;
import com.jhddt.module.review.entity.ReviewRecordEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.service.ReviewService;
import com.jhddt.module.review.vo.ReviewRecordDetailVO;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PostMapping("/essay/{id}")
    public Result<ReviewRecordDetailVO> reviewEssay(
            @Parameter(description = "作文ID", required = true, example = "100")
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long userId = authentication == null ? null : (Long) authentication.getPrincipal();
            ReviewRecordDetailVO detail = reviewService.reviewEssayAndSave(id, userId);
            return Result.success("评审成功", detail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("评审失败: {}", e.getMessage(), e);
            return Result.error("评审失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询评审记录详情", description = "根据评审记录ID查询详细信息（包含各维度得分和评论）")
    @GetMapping("/record/{reviewId}")
    public Result<ReviewRecordDetailVO> getReviewRecordDetail(
            @Parameter(description = "评审记录ID", required = true, example = "1")
            @PathVariable Long reviewId) {
        try {
            ReviewRecordDetailVO detail = reviewService.getReviewDetail(reviewId);
            if (detail == null) {
                return Result.error(404, "评审记录不存在");
            }
            return Result.success("查询成功", detail);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询作文的所有评审记录", description = "根据作文ID查询该作文的所有评审记录列表（按创建时间倒序）")
    @GetMapping("/essay/{essayId}/records")
    public Result<List<ReviewRecordEntity>> getEssayReviewRecords(
            @Parameter(description = "作文ID", required = true, example = "100")
            @PathVariable Long essayId) {
        try {
            return Result.success("查询成功", reviewService.listByEssayId(essayId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分页查询评审记录列表", description = "支持按状态和评审者类型筛选，结果按创建时间倒序排列")
    @GetMapping("/records")
    public Result<Page<ReviewRecordEntity>> pageReviewRecords(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "评审状态（可选）：0-INIT，1-PROCESSING，2-SUCCESS，3-FAIL，4-TIMEOUT")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "评审者类型（可选）：0-AI，1-教师")
            @RequestParam(required = false) Integer reviewerType) {
        try {
            return Result.success("查询成功", reviewService.pageRecords(page, pageSize, status, reviewerType));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // =========================
    // 评分维度配置相关（你要的“可修改配置”接口）
    // =========================

    @Operation(summary = "查询评分维度配置", description = "查询 score_dimension 中的评分维度配置（可选：仅查询启用的维度）")
    @GetMapping("/dimensions")
    public Result<List<ScoreDimensionEntity>> listDimensions(
            @Parameter(description = "是否仅返回启用维度（status=1）", example = "true")
            @RequestParam(required = false) Boolean enabledOnly) {
        try {
            return Result.success("查询成功", reviewService.listDimensions(enabledOnly));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增评分维度", description = "新增一条评分维度配置（dimensionName/weight/maxScore 必填）")
    @PostMapping("/dimensions")
    public Result<ScoreDimensionEntity> createDimension(@RequestBody ScoreDimensionEntity dim) {
        try {
            return Result.success("创建成功", reviewService.createDimension(dim));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新评分维度", description = "更新评分维度配置（支持部分字段更新）")
    @PutMapping("/dimensions/{id}")
    public Result<Void> updateDimension(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ScoreDimensionEntity dim) {
        try {
            boolean ok = reviewService.updateDimension(id, dim);
            return ok ? Result.success() : Result.error("更新失败（维度不存在或已删除）");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用评分维度", description = "通过 enabled=true/false 控制 status=1/0")
    @PatchMapping("/dimensions/{id}/status")
    public Result<Void> updateDimensionStatus(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "是否启用", required = true, example = "true")
            @RequestParam Boolean enabled) {
        try {
            boolean ok = reviewService.updateDimensionStatus(id, enabled);
            return ok ? Result.success() : Result.error("更新失败（维度不存在或已删除）");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除评分维度", description = "逻辑删除评分维度（is_deleted=1）")
    @DeleteMapping("/dimensions/{id}")
    public Result<Void> deleteDimension(
            @Parameter(description = "维度ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            boolean ok = reviewService.deleteDimension(id);
            return ok ? Result.success() : Result.error("删除失败（维度不存在或已删除）");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}

