package com.jhddt.module.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.common.enums.EssayStatus;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.service.EssayService;
import com.jhddt.module.review.dto.ReviewResult;
import com.jhddt.module.review.dto.ReviewRuleSnapshotDTO;
import com.jhddt.module.review.dto.ReviewTaskMessage;
import com.jhddt.module.review.dto.BatchReviewItemResult;
import com.jhddt.module.review.dto.BatchReviewResponse;
import com.jhddt.module.review.dto.StructuredReviewPayloadDTO;
import com.jhddt.module.review.dto.StructuredScoreItemDTO;
import com.jhddt.module.review.dto.TeacherManualReviewRequest;
import com.jhddt.module.review.dto.TeacherManualCommentInput;
import com.jhddt.module.review.dto.TeacherScoreInput;
import com.jhddt.module.review.dto.TextCorrectionDTO;
import com.jhddt.module.review.entity.BatchReviewTaskEntity;
import com.jhddt.module.review.entity.ReviewCommentEntity;
import com.jhddt.module.review.entity.ReviewRecordEntity;
import com.jhddt.module.review.entity.ReviewRuleEntity;
import com.jhddt.module.review.entity.ReviewScoreEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.entity.TextCorrectionEntity;
import com.jhddt.module.review.mapper.ReviewMapper;
import com.jhddt.module.review.vo.ReviewCommentVO;
import com.jhddt.module.review.vo.ReviewRecordDetailVO;
import com.jhddt.module.review.vo.ReviewRecordVO;
import com.jhddt.module.review.vo.ReviewScoreVO;
import com.jhddt.module.review.vo.ReviewStatusVO;
import com.jhddt.common.util.JwtUtil;
import com.jhddt.config.security.JwtProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * Review 模块统一 Service（按功能聚合）
 * <p>
 * 只保留一个 Service：包含 AI 调用、评审落库、详情查询、评分维度配置 CRUD。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ChatClient chatClient;
    private final ReviewMapper reviewMapper;
    private final EssayService essayService;
    @Qualifier("correctServiceRestTemplate")
    private final RestTemplate correctServiceRestTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Value("${correct.service.url:http://127.0.0.1:8001}")
    private String correctServiceUrl;
    @Value("${review.mq.exchange}")
    private String reviewExchange;
    @Value("${review.mq.routing-key}")
    private String reviewRoutingKey;
    @Value("${review.cache.status-ttl-minutes:60}")
    private long reviewStatusTtlMinutes;
    @Value("${review.cache.detail-ttl-minutes:120}")
    private long reviewDetailTtlMinutes;
    @Value("${review.cache.lock-ttl-minutes:15}")
    private long reviewLockTtlMinutes;

    // =========================
    // 评审主流程
    // =========================

    /**
     * 发起批改（异步处理）
     * 立即返回 reviewId 和 PROCESSING 状态，实际批改在后台异步执行
     */
    public ReviewRecordDetailVO reviewEssayAndSave(Long essayId, Long userId, Long ruleId) {
        return reviewEssayAndSave(essayId, userId, ruleId, null);
    }

    public ReviewRecordDetailVO reviewEssayAndSave(Long essayId, Long userId, Long ruleId, Long batchTaskId) {
        // 打印当前token的剩余有效期
        printTokenRemainingValidity();
        
        // 1) 查询作文并校验归属
        EssayEntity essay = essayService.getById(essayId);
        if (essay == null) {
            throw new IllegalArgumentException("作文不存在");
        }
        if (userId != null && essay.getUserId() != null && !Objects.equals(essay.getUserId(), userId) && !hasReviewManagePrivilege()) {
            throw new IllegalArgumentException("无权操作此作文");
        }

        // 2) 取内容
        String essayContent = essay.getFinalContent();
        if (essayContent == null || essayContent.trim().isEmpty()) {
            essayContent = essay.getOriginalContent();
        }
        if (essayContent == null || essayContent.trim().isEmpty()) {
            throw new IllegalArgumentException("作文内容为空，无法评审");
        }

        if (!tryAcquireReviewLock(essayId)) {
            throw new IllegalArgumentException("该作文正在批改，请勿重复提交");
        }

        ReviewRuleEntity selectedRule = getReviewRuleForPrompt(ruleId);
        ReviewRuleSnapshotDTO ruleSnapshot = buildRuleSnapshot(selectedRule);

        // 3) 构建提示词（并作为 rule_version 快照存档）
        String prompt = buildReviewPrompt(essay.getTitle(), essayContent, selectedRule);

        // 4) 先插入 review_record（PROCESSING）
        LocalDateTime startTime = LocalDateTime.now();
        ReviewRecordEntity record = ReviewRecordEntity.builder()
                .essayId(essayId)
                .batchTaskId(batchTaskId)
                .reviewerType(0)
                .ruleVersion(serializeRuleSnapshot(ruleSnapshot))
                .modelVersion("deepseek-chat")
                .startTime(startTime)
                .status(1) // PROCESSING
                .retryCount(0)
                .build();
        reviewMapper.insertReviewRecord(record);
        Long reviewId = record.getReviewId();
        reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder()
                .reviewId(reviewId)
                .taskId(reviewId)
                .build());
        cacheReviewStatus(ReviewStatusVO.builder()
                .reviewId(reviewId)
                .essayId(essayId)
                .status(1)
                .updateTime(startTime)
                .build());

        // 5) 将作文状态切到批改中，再投递 MQ 消息
        updateEssayStatus(essayId, EssayStatus.CORRECTING);
        try {
            rabbitTemplate.convertAndSend(
                    reviewExchange,
                    reviewRoutingKey,
                    ReviewTaskMessage.builder()
                            .reviewId(reviewId)
                            .essayId(essayId)
                            .ruleId(ruleId)
                            .batchTaskId(batchTaskId)
                            .build());
        } catch (Exception e) {
            log.error("发送批改任务到 RabbitMQ 失败，reviewId={}, essayId={}, error={}", reviewId, essayId, e.getMessage(), e);
            reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(3)
                    .endTime(LocalDateTime.now())
                    .errorMsg("消息投递失败: " + e.getMessage())
                    .build());
            updateEssayStatus(essayId, EssayStatus.SUBMITTED);
            cacheReviewStatus(ReviewStatusVO.builder()
                    .reviewId(reviewId)
                    .essayId(essayId)
                    .status(3)
                    .errorMsg("消息投递失败: " + e.getMessage())
                    .updateTime(LocalDateTime.now())
                    .build());
            releaseReviewLock(essayId);
            throw new RuntimeException("批改任务投递失败，请稍后重试");
        }

        // 6) 立即返回，包含 reviewId 和 PROCESSING 状态
        ReviewRecordDetailVO vo = ReviewRecordDetailVO.builder()
                .reviewId(reviewId)
                .essayId(essayId)
                .essayTitle(essay.getTitle())
                .reviewerType(0)
                .modelVersion("deepseek-chat")
                .startTime(startTime)
                .status(1) // PROCESSING
                .ruleId(ruleSnapshot.getRuleId())
                .ruleName(ruleSnapshot.getRuleName())
                .reviewType(ruleSnapshot.getReviewType())
                .gradeLevel(ruleSnapshot.getGradeLevel())
                .build();
        return vo;
    }

    public void processReviewTask(Long reviewId, Long essayId, Long ruleId, Long batchTaskId) {
        EssayEntity essay = essayService.getById(essayId);
        if (essay == null) {
            log.error("批改任务对应作文不存在，reviewId={}, essayId={}", reviewId, essayId);
            reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(3)
                    .endTime(LocalDateTime.now())
                    .errorMsg("作文不存在")
                    .build());
            cacheReviewStatus(ReviewStatusVO.builder()
                    .reviewId(reviewId)
                    .essayId(essayId)
                    .status(3)
                    .errorMsg("作文不存在")
                    .updateTime(LocalDateTime.now())
                    .build());
            markBatchTaskFail(batchTaskId);
            releaseReviewLock(essayId);
            return;
        }

        String essayContent = essay.getFinalContent();
        if (essayContent == null || essayContent.trim().isEmpty()) {
            essayContent = essay.getOriginalContent();
        }
        if (essayContent == null || essayContent.trim().isEmpty()) {
            reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(3)
                    .endTime(LocalDateTime.now())
                    .errorMsg("作文内容为空，无法评审")
                    .build());
            updateEssayStatus(essayId, EssayStatus.SUBMITTED);
            cacheReviewStatus(ReviewStatusVO.builder()
                    .reviewId(reviewId)
                    .essayId(essayId)
                    .status(3)
                    .errorMsg("作文内容为空，无法评审")
                    .updateTime(LocalDateTime.now())
                    .build());
            markBatchTaskFail(batchTaskId);
            releaseReviewLock(essayId);
            return;
        }

        ReviewRuleEntity selectedRule = getReviewRuleForPrompt(ruleId);
        String prompt = buildReviewPrompt(essay.getTitle(), essayContent, selectedRule);

        try {
            log.info("开始消费 RabbitMQ 批改任务，reviewId={}, essayId={}", reviewId, essayId);

            // 1) 先调用文本纠错服务并保存（不影响主流程，失败仅记录日志）
            try {
                callTextCorrectionAndSave(reviewId, essayContent);
            } catch (Exception e) {
                log.error("文本纠错失败，但不影响批改流程，reviewId={}, error={}", reviewId, e.getMessage(), e);
            }

            // 2) 调用 AI 批改
            ReviewResult aiResult = callAi(prompt);

            // 3) 保存各维度得分（先保存，用于计算总分）
            List<ReviewScoreEntity> savedScores = saveDimensionScoresIfAny(reviewId, aiResult, selectedRule);

            // 4) 计算总分：从 review_score 表中同一 review_id 的所有 score 字段求和
            BigDecimal calculatedTotalScore = calculateTotalScoreFromSavedScores(reviewId);
            // 如果计算出总分就用计算的，否则用 AI 返回的总分作为兜底
            BigDecimal totalScore = calculatedTotalScore != null
                    ? calculatedTotalScore
                    : aiResult.getStructuredPayload() != null && aiResult.getStructuredPayload().getTotalScore() != null
                    ? aiResult.getStructuredPayload().getTotalScore()
                    : parseTotalScore(aiResult.getScore());

            // 5) 更新记录（SUCCESS）
            ReviewRecordEntity record = ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(2) // SUCCESS
                    .endTime(LocalDateTime.now())
                    .totalScore(totalScore)
                    .build();
            reviewMapper.updateReviewRecordById(record);
            updateEssayStatus(essayId, EssayStatus.CORRECTED);

            // 6) 保存评论（总评/建议/修改意见），并填充位置信息
            saveCommentsWithPosition(reviewId, aiResult.getReviewContent(), essayContent);
            evictReviewDetailCache(reviewId);
            cacheReviewStatus(ReviewStatusVO.builder()
                    .reviewId(reviewId)
                    .essayId(essayId)
                    .status(2)
                    .totalScore(totalScore)
                    .updateTime(LocalDateTime.now())
                    .build());
            markBatchTaskSuccess(batchTaskId);

            log.info("异步批改完成，reviewId={}, essayId={}, totalScore={}", reviewId, essayId, totalScore);
        } catch (Exception e) {
            // FAIL
            log.error("异步批改失败，reviewId={}, essayId={}, error={}", reviewId, essayId, e.getMessage(), e);
            ReviewRecordEntity record = ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(3) // FAIL
                    .endTime(LocalDateTime.now())
                    .errorMsg(e.getMessage())
                    .build();
            reviewMapper.updateReviewRecordById(record);
            updateEssayStatus(essayId, EssayStatus.SUBMITTED);
            evictReviewDetailCache(reviewId);
            cacheReviewStatus(ReviewStatusVO.builder()
                    .reviewId(reviewId)
                    .essayId(essayId)
                    .status(3)
                    .errorMsg(e.getMessage())
                    .updateTime(LocalDateTime.now())
                    .build());
            markBatchTaskFail(batchTaskId);
        } finally {
            releaseReviewLock(essayId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchReviewResponse batchReviewAndSave(List<Long> essayIds, Long userId, Long ruleId) {
        if (essayIds == null || essayIds.isEmpty()) {
            throw new IllegalArgumentException("essayIds 不能为空");
        }
        List<Long> distinctIds = essayIds.stream().filter(Objects::nonNull).distinct().toList();
        BatchReviewTaskEntity task = BatchReviewTaskEntity.builder()
                .creatorId(userId)
                .ruleId(ruleId)
                .totalCount(distinctIds.size())
                .successCount(0)
                .failCount(0)
                .status(0)
                .startTime(LocalDateTime.now())
                .build();
        reviewMapper.insertBatchTask(task);

        List<BatchReviewItemResult> items = new ArrayList<>();
        int success = 0;
        int fail = 0;
        for (Long essayId : distinctIds) {
            try {
                ReviewRecordDetailVO detail = reviewEssayAndSave(essayId, userId, ruleId, task.getTaskId());
                success++;
                items.add(BatchReviewItemResult.builder()
                        .essayId(essayId)
                        .reviewId(detail.getReviewId())
                        .success(true)
                        .message("已提交批改任务")
                        .build());
            } catch (Exception e) {
                fail++;
                items.add(BatchReviewItemResult.builder()
                        .essayId(essayId)
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }
        }

        reviewMapper.updateBatchTaskById(BatchReviewTaskEntity.builder()
                .taskId(task.getTaskId())
                .successCount(success)
                .failCount(fail)
                .status(fail == 0 ? 1 : 2)
                .endTime(LocalDateTime.now())
                .errorMsg(fail == 0 ? null : "部分作文提交失败")
                .build());

        return BatchReviewResponse.builder()
                .batchTaskId(task.getTaskId())
                .totalCount(distinctIds.size())
                .successCount(success)
                .failCount(fail)
                .items(items)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewRecordDetailVO createTeacherManualReview(Long teacherId, TeacherManualReviewRequest request) {
        if (request == null || request.getEssayId() == null) {
            throw new IllegalArgumentException("essayId 不能为空");
        }
        EssayEntity essay = essayService.getById(request.getEssayId());
        if (essay == null) {
            throw new IllegalArgumentException("作文不存在");
        }

        ReviewRecordEntity sourceRecord = null;
        if (request.getSourceReviewId() != null) {
            sourceRecord = reviewMapper.selectReviewRecordById(request.getSourceReviewId());
            if (sourceRecord == null) {
                throw new IllegalArgumentException("来源评审记录不存在");
            }
            if (!Objects.equals(sourceRecord.getEssayId(), request.getEssayId())) {
                throw new IllegalArgumentException("来源评审与作文不匹配");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ReviewRecordEntity record = ReviewRecordEntity.builder()
                .essayId(request.getEssayId())
                .sourceReviewId(request.getSourceReviewId())
                .reviewerType(1)
                .reviewerId(teacherId)
                .ruleVersion(request.getRuleVersion() != null ? request.getRuleVersion() : (sourceRecord != null ? sourceRecord.getRuleVersion() : "教师手动批改"))
                .modelVersion("teacher-manual")
                .startTime(now)
                .endTime(now)
                .status(2)
                .retryCount(0)
                .build();
        reviewMapper.insertReviewRecord(record);
        reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder().reviewId(record.getReviewId()).taskId(record.getReviewId()).build());

        saveTeacherScores(record.getReviewId(), request.getScores());
        saveTeacherComments(record.getReviewId(), request.getSummary(), request.getSuggestions(), request.getRevisions(), request.getAnnotations());
        BigDecimal total = calculateTotalScoreFromSavedScores(record.getReviewId());
        reviewMapper.updateReviewRecordById(ReviewRecordEntity.builder()
                .reviewId(record.getReviewId())
                .totalScore(total)
                .build());
        updateEssayStatus(request.getEssayId(), EssayStatus.CORRECTED);

        return getReviewDetail(record.getReviewId(), essay.getUserId());
    }

    private void updateEssayStatus(Long essayId, EssayStatus status) {
        if (essayId == null || status == null) {
            return;
        }
        EssayEntity essay = new EssayEntity();
        essay.setId(essayId);
        essay.setStatus(status);
        essayService.updateById(essay);
    }

    /**
     * 对作文进行文本纠错并保存到数据库
     * @param essayId 作文ID
     * @param reviewId 评审记录ID（可选，如果不提供则创建新的评审记录）
     * @param userId 用户ID
     * @return 包含 reviewId 和 correctionCount 的 Map
     */
    public Map<String, Object> correctEssay(Long essayId, Long reviewId, Long userId) {
        // 1) 查询作文并校验归属
        EssayEntity essay = essayService.getById(essayId);
        if (essay == null) {
            throw new IllegalArgumentException("作文不存在");
        }
        if (userId != null && essay.getUserId() != null && !Objects.equals(essay.getUserId(), userId) && !hasReviewManagePrivilege()) {
            throw new IllegalArgumentException("无权操作此作文");
        }

        // 2) 获取作文内容
        String essayContent = essay.getFinalContent();
        if (essayContent == null || essayContent.trim().isEmpty()) {
            essayContent = essay.getOriginalContent();
        }
        if (essayContent == null || essayContent.trim().isEmpty()) {
            throw new IllegalArgumentException("作文内容为空，无法纠错");
        }

        // 3) 如果没有提供 reviewId，创建一个新的评审记录
        if (reviewId == null) {
            LocalDateTime startTime = LocalDateTime.now();
            ReviewRecordEntity record = ReviewRecordEntity.builder()
                    .essayId(essayId)
                    .reviewerType(0)
                    .ruleVersion("文本纠错")
                    .modelVersion("text-correction-service")
                    .startTime(startTime)
                    .status(1) // PROCESSING
                    .retryCount(0)
                    .build();
            reviewMapper.insertReviewRecord(record);
            reviewId = record.getReviewId();
            log.info("创建新的评审记录用于文本纠错，reviewId={}, essayId={}", reviewId, essayId);
        } else {
            // 验证 reviewId 是否存在且属于该作文
            ReviewRecordEntity existingRecord = reviewMapper.selectReviewRecordById(reviewId);
            if (existingRecord == null) {
                throw new IllegalArgumentException("评审记录不存在");
            }
            if (!existingRecord.getEssayId().equals(essayId)) {
                throw new IllegalArgumentException("评审记录与作文不匹配");
            }
            // 验证权限
            if (userId != null && !hasReviewManagePrivilege()) {
                EssayEntity recordEssay = essayService.getById(existingRecord.getEssayId());
                if (recordEssay == null || !recordEssay.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("无权访问此评审记录");
                }
            }
        }

        // 4) 调用文本纠错服务并保存
        callTextCorrectionAndSave(reviewId, essayContent);

        // 5) 查询保存的纠错记录数量
        List<TextCorrectionEntity> corrections = reviewMapper.selectTextCorrectionsByReviewId(reviewId);
        int correctionCount = corrections.size();

        // 6) 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviewId", reviewId);
        result.put("essayId", essayId);
        result.put("correctionCount", correctionCount);
        
        log.info("文本纠错完成，reviewId={}, essayId={}, correctionCount={}", reviewId, essayId, correctionCount);
        return result;
    }

    public ReviewRecordDetailVO getReviewDetail(Long reviewId, Long userId) {
        ReviewRecordEntity record = reviewMapper.selectReviewRecordById(reviewId);
        if (record == null) {
            return null;
        }

        // 验证权限：检查作文是否属于当前用户
        if (record.getEssayId() != null && userId != null && !hasReviewManagePrivilege()) {
            EssayEntity essay = essayService.getById(record.getEssayId());
            if (essay == null || !essay.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权访问此评审记录");
            }
        }

        List<ReviewScoreEntity> scores = reviewMapper.selectScoresByReviewId(reviewId);
        List<ReviewCommentEntity> comments = reviewMapper.selectCommentsByReviewId(reviewId);
        List<TextCorrectionEntity> corrections = reviewMapper.selectTextCorrectionsByReviewId(reviewId);

        // dimensionId -> name
        Map<Long, String> dimNameMap = reviewMapper.selectAllDimensions().stream()
                .collect(Collectors.toMap(ScoreDimensionEntity::getDimensionId, ScoreDimensionEntity::getDimensionName, (a, b) -> a));

        List<ReviewScoreVO> scoreVOs = scores.stream()
                .map(s -> ReviewScoreVO.builder()
                        .scoreId(s.getScoreId())
                        .dimensionId(s.getDimensionId())
                        .dimensionName(dimNameMap.get(s.getDimensionId()))
                        .weightSnapshot(s.getWeightSnapshot())
                        .score(s.getScore())
                        .build())
                .toList();

        List<ReviewCommentVO> commentVOs = comments.stream()
                .map(c -> ReviewCommentVO.builder()
                        .commentId(c.getCommentId())
                        .commentType(c.getCommentType())
                        .content(c.getContent())
                        .startOffset(c.getStartOffset())
                        .endOffset(c.getEndOffset())
                        .relatedText(c.getRelatedText())
                        .createTime(c.getCreateTime())
                        .build())
                .toList();

        List<TextCorrectionDTO> correctionDTOs = corrections.stream()
                .map(tc -> TextCorrectionDTO.builder()
                        .originalText(tc.getOriginalText())
                        .correctedText(tc.getCorrectedText())
                        .startOffset(tc.getStartOffset())
                        .endOffset(tc.getEndOffset())
                        .errorType(tc.getErrorType())
                        .suggestion(tc.getSuggestion())
                        .build())
                .toList();

        // 获取作文标题
        String essayTitle = null;
        if (record.getEssayId() != null) {
            EssayEntity essay = essayService.getById(record.getEssayId());
            if (essay != null) {
                essayTitle = essay.getTitle();
            }
        }

        ReviewRuleSnapshotDTO ruleSnapshot = parseRuleSnapshot(record.getRuleVersion());
        ReviewVersionMeta versionMeta = resolveReviewVersionMeta(record.getEssayId(), record.getReviewId());

        ReviewRecordDetailVO detail = ReviewRecordDetailVO.builder()
                .reviewId(record.getReviewId())
                .essayId(record.getEssayId())
                .reviewerType(record.getReviewerType())
                .reviewerId(record.getReviewerId())
                .modelVersion(record.getModelVersion())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .totalScore(record.getTotalScore())
                .status(record.getStatus())
                .errorMsg(record.getErrorMsg())
                .createTime(record.getCreateTime())
                .essayTitle(essayTitle)
                .ruleId(ruleSnapshot.getRuleId())
                .ruleName(ruleSnapshot.getRuleName())
                .reviewType(ruleSnapshot.getReviewType())
                .gradeLevel(ruleSnapshot.getGradeLevel())
                .topicRequirement(ruleSnapshot.getTopicRequirement())
                .beautifyLevel(ruleSnapshot.getBeautifyLevel())
                .customRequirement(ruleSnapshot.getCustomRequirement())
                .deductionDetail(ruleSnapshot.getDeductionDetail())
                .reviewVersion(versionMeta.reviewVersion())
                .latestVersion(versionMeta.latestVersion())
                .scores(scoreVOs)
                .comments(commentVOs)
                .textCorrections(correctionDTOs)
                .build();
        cacheReviewDetail(detail);
        return detail;
    }

    public ReviewStatusVO getReviewStatus(Long reviewId, Long userId) {
        ReviewRecordEntity record = reviewMapper.selectReviewRecordById(reviewId);
        if (record == null) {
            return null;
        }
        if (record.getEssayId() != null && userId != null && !hasReviewManagePrivilege()) {
            EssayEntity essay = essayService.getById(record.getEssayId());
            if (essay == null || !essay.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权访问此评审记录");
            }
        }

        ReviewStatusVO cached = getCachedReviewStatus(reviewId);
        if (cached != null) {
            return cached;
        }

        ReviewStatusVO status = ReviewStatusVO.builder()
                .reviewId(record.getReviewId())
                .essayId(record.getEssayId())
                .status(record.getStatus())
                .totalScore(record.getTotalScore())
                .errorMsg(record.getErrorMsg())
                .updateTime(record.getEndTime() != null ? record.getEndTime() : record.getStartTime())
                .build();
        cacheReviewStatus(status);
        return status;
    }

    public List<ReviewRecordVO> listByEssayId(Long essayId, Long userId) {
        // 验证权限：检查作文是否属于当前用户
        if (essayId != null && userId != null && !hasReviewManagePrivilege()) {
            EssayEntity essay = essayService.getById(essayId);
            if (essay == null || !essay.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权访问此作文的评审记录");
            }
        }
        return enrichReviewRecordVos(reviewMapper.selectReviewRecordsByEssayId(essayId).stream()
                .map(this::toReviewRecordVO)
                .toList());
    }

    public Page<ReviewRecordVO> pageRecords(Integer page, Integer pageSize, Integer status, Integer reviewerType, Long essayId, Long userId) {
        Page<ReviewRecordVO> p = new Page<>(page, pageSize);
        Long queryUserId = hasReviewManagePrivilege() ? null : userId;
        List<ReviewRecordVO> records = reviewMapper.selectReviewRecordsPageByUserId(p, status, reviewerType, queryUserId, essayId);
        p.setRecords(enrichReviewRecordVos(records));
        return p;
    }

    public boolean deleteReviewRecord(Long reviewId, Long userId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("reviewId 不能为空");
        }
        
        // 验证权限：检查评审记录对应的作文是否属于当前用户
        ReviewRecordEntity record = reviewMapper.selectReviewRecordById(reviewId);
        if (record == null) {
            return false;
        }
        
        if (record.getEssayId() != null && userId != null && !hasReviewManagePrivilege()) {
            EssayEntity essay = essayService.getById(record.getEssayId());
            if (essay == null || !essay.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权删除此评审记录");
            }
        }
        
        boolean deleted = reviewMapper.deleteReviewRecordLogic(reviewId) > 0;
        if (deleted) {
            evictReviewDetailCache(reviewId);
            evictReviewStatusCache(reviewId);
        }
        return deleted;
    }

    private void saveTeacherScores(Long reviewId, List<TeacherScoreInput> scores) {
        if (scores == null || scores.isEmpty()) {
            return;
        }
        List<ReviewScoreEntity> entities = scores.stream()
                .filter(s -> s.getDimensionId() != null && s.getScore() != null)
                .map(s -> ReviewScoreEntity.builder()
                        .reviewId(reviewId)
                        .dimensionId(s.getDimensionId())
                        .weightSnapshot(null)
                        .score(s.getScore())
                        .build())
                .toList();
        if (!entities.isEmpty()) {
            reviewMapper.insertReviewScores(entities);
        }
    }

    private void saveTeacherComments(
            Long reviewId,
            String summary,
            String suggestions,
            String revisions,
            List<TeacherManualCommentInput> annotations
    ) {
        List<ReviewCommentEntity> comments = new ArrayList<>();
        if (summary != null && !summary.isBlank()) {
            comments.add(ReviewCommentEntity.builder().reviewId(reviewId).commentType(1).content(summary).build());
        }
        if (suggestions != null && !suggestions.isBlank()) {
            comments.add(ReviewCommentEntity.builder().reviewId(reviewId).commentType(2).content(suggestions).build());
        }
        if (revisions != null && !revisions.isBlank()) {
            comments.add(ReviewCommentEntity.builder().reviewId(reviewId).commentType(3).content(revisions).build());
        }
        if (annotations != null && !annotations.isEmpty()) {
            for (TeacherManualCommentInput item : annotations) {
                if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                Integer type = item.getCommentType();
                if (type == null || (type != 2 && type != 3)) {
                    type = 2;
                }
                comments.add(ReviewCommentEntity.builder()
                        .reviewId(reviewId)
                        .commentType(type)
                        .content(item.getContent())
                        .startOffset(item.getStartOffset())
                        .endOffset(item.getEndOffset())
                        .relatedText(item.getRelatedText())
                        .build());
            }
        }
        if (!comments.isEmpty()) {
            reviewMapper.insertReviewComments(comments);
        }
    }

    private void markBatchTaskSuccess(Long batchTaskId) {
        if (batchTaskId == null) {
            return;
        }
        BatchReviewTaskEntity task = reviewMapper.selectBatchTaskById(batchTaskId);
        if (task == null) {
            return;
        }
        int nextSuccess = Optional.ofNullable(task.getSuccessCount()).orElse(0) + 1;
        int total = Optional.ofNullable(task.getTotalCount()).orElse(0);
        int fail = Optional.ofNullable(task.getFailCount()).orElse(0);
        boolean finished = nextSuccess + fail >= total && total > 0;
        reviewMapper.updateBatchTaskById(BatchReviewTaskEntity.builder()
                .taskId(batchTaskId)
                .successCount(nextSuccess)
                .status(finished ? 1 : 0)
                .endTime(finished ? LocalDateTime.now() : null)
                .build());
    }

    private void markBatchTaskFail(Long batchTaskId) {
        if (batchTaskId == null) {
            return;
        }
        BatchReviewTaskEntity task = reviewMapper.selectBatchTaskById(batchTaskId);
        if (task == null) {
            return;
        }
        int nextFail = Optional.ofNullable(task.getFailCount()).orElse(0) + 1;
        int total = Optional.ofNullable(task.getTotalCount()).orElse(0);
        int success = Optional.ofNullable(task.getSuccessCount()).orElse(0);
        boolean finished = success + nextFail >= total && total > 0;
        reviewMapper.updateBatchTaskById(BatchReviewTaskEntity.builder()
                .taskId(batchTaskId)
                .failCount(nextFail)
                .status(finished ? 2 : 0)
                .endTime(finished ? LocalDateTime.now() : null)
                .errorMsg("存在失败记录")
                .build());
    }

    private boolean tryAcquireReviewLock(Long essayId) {
        if (essayId == null) {
            return true;
        }
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                buildReviewLockKey(essayId),
                "1",
                Duration.ofMinutes(reviewLockTtlMinutes));
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseReviewLock(Long essayId) {
        if (essayId != null) {
            stringRedisTemplate.delete(buildReviewLockKey(essayId));
        }
    }

    private void cacheReviewStatus(ReviewStatusVO status) {
        if (status == null || status.getReviewId() == null) {
            return;
        }
        writeJsonCache(buildReviewStatusKey(status.getReviewId()), status, Duration.ofMinutes(reviewStatusTtlMinutes));
    }

    private ReviewStatusVO getCachedReviewStatus(Long reviewId) {
        return readJsonCache(buildReviewStatusKey(reviewId), ReviewStatusVO.class);
    }

    private void evictReviewStatusCache(Long reviewId) {
        if (reviewId != null) {
            stringRedisTemplate.delete(buildReviewStatusKey(reviewId));
        }
    }

    private void cacheReviewDetail(ReviewRecordDetailVO detail) {
        if (detail == null || detail.getReviewId() == null) {
            return;
        }
        writeJsonCache(buildReviewDetailKey(detail.getReviewId()), detail, Duration.ofMinutes(reviewDetailTtlMinutes));
    }

    private ReviewRecordDetailVO getCachedReviewDetail(Long reviewId) {
        return readJsonCache(buildReviewDetailKey(reviewId), ReviewRecordDetailVO.class);
    }

    private void evictReviewDetailCache(Long reviewId) {
        if (reviewId != null) {
            stringRedisTemplate.delete(buildReviewDetailKey(reviewId));
        }
    }

    private <T> void writeJsonCache(String key, T value, Duration ttl) {
        if (key == null || value == null || ttl == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            log.warn("写入 Redis 缓存失败，key={}, error={}", key, e.getMessage());
        }
    }

    private <T> T readJsonCache(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("读取 Redis 缓存失败，key={}, error={}", key, e.getMessage());
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private String buildReviewLockKey(Long essayId) {
        return "lock:review:essay:" + essayId;
    }

    private String buildReviewStatusKey(Long reviewId) {
        return "review:status:" + reviewId;
    }

    private String buildReviewDetailKey(Long reviewId) {
        return "review:detail:" + reviewId;
    }

    private ReviewRuleSnapshotDTO buildRuleSnapshot(ReviewRuleEntity selectedRule) {
        if (selectedRule == null) {
            return ReviewRuleSnapshotDTO.builder()
                    .ruleId(null)
                    .ruleName("系统默认通用细则")
                    .reviewType("通用作文")
                    .snapshotLabel("系统默认通用细则")
                    .build();
        }
        return ReviewRuleSnapshotDTO.builder()
                .ruleId(selectedRule.getRuleId())
                .ruleName(selectedRule.getRuleName())
                .reviewType(selectedRule.getReviewType())
                .gradeLevel(selectedRule.getGradeLevel())
                .topicRequirement(selectedRule.getTopicRequirement())
                .beautifyLevel(selectedRule.getBeautifyLevel())
                .customRequirement(selectedRule.getCustomRequirement())
                .deductionDetail(selectedRule.getDeductionDetail())
                .promptTemplate(selectedRule.getPromptTemplate())
                .snapshotLabel(buildRuleSnapshotLabel(selectedRule))
                .build();
    }

    private String buildRuleSnapshotLabel(ReviewRuleEntity selectedRule) {
        if (selectedRule == null) {
            return "系统默认通用细则";
        }
        StringBuilder label = new StringBuilder();
        if (selectedRule.getRuleName() != null && !selectedRule.getRuleName().isBlank()) {
            label.append(selectedRule.getRuleName());
        } else {
            label.append("未命名细则");
        }
        if (selectedRule.getGradeLevel() != null && !selectedRule.getGradeLevel().isBlank()) {
            label.append(" / ").append(selectedRule.getGradeLevel());
        }
        if (selectedRule.getReviewType() != null && !selectedRule.getReviewType().isBlank()) {
            label.append(" / ").append(selectedRule.getReviewType());
        }
        return label.toString();
    }

    private String serializeRuleSnapshot(ReviewRuleSnapshotDTO snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("序列化批改细则快照失败，使用降级文本，error={}", e.getMessage());
            return snapshot.getSnapshotLabel();
        }
    }

    private ReviewRuleSnapshotDTO parseRuleSnapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            return ReviewRuleSnapshotDTO.builder()
                    .ruleName("未记录批改细则")
                    .snapshotLabel("未记录批改细则")
                    .build();
        }
        try {
            ReviewRuleSnapshotDTO snapshot = objectMapper.readValue(raw, ReviewRuleSnapshotDTO.class);
            if (snapshot.getSnapshotLabel() == null || snapshot.getSnapshotLabel().isBlank()) {
                snapshot.setSnapshotLabel(snapshot.getRuleName());
            }
            normalizeRuleSnapshot(snapshot);
            return snapshot;
        } catch (Exception e) {
            ReviewRuleSnapshotDTO snapshot = ReviewRuleSnapshotDTO.builder()
                    .ruleName(extractLegacyRuleName(raw))
                    .reviewType(extractLegacyRuleField(raw, "批改类型"))
                    .gradeLevel(extractLegacyRuleField(raw, "适用学段"))
                    .topicRequirement(firstNonBlank(
                            extractLegacyRuleField(raw, "题干要求"),
                            extractLegacyRuleField(raw, "题目要求")
                    ))
                    .beautifyLevel(firstNonBlank(
                            extractLegacyRuleField(raw, "内容润色强度参考"),
                            extractLegacyRuleField(raw, "润色等级")
                    ))
                    .customRequirement(firstNonBlank(
                            extractLegacyRuleField(raw, "自定义批改要求"),
                            extractLegacyRuleField(raw, "细则附加要求"),
                            extractLegacyRuleField(raw, "附加要求")
                    ))
                    .deductionDetail(firstNonBlank(
                            extractLegacyRuleField(raw, "扣分细则"),
                            extractLegacyRuleField(raw, "扣分要求")
                    ))
                    .snapshotLabel("历史批改细则")
                    .promptTemplate(raw)
                    .build();
            normalizeRuleSnapshot(snapshot);
            return snapshot;
        }
    }

    private void normalizeRuleSnapshot(ReviewRuleSnapshotDTO snapshot) {
        if (snapshot == null) {
            return;
        }
        snapshot.setRuleName(cleanRuleDisplayName(snapshot.getRuleName()));
        snapshot.setReviewType(cleanRuleField(snapshot.getReviewType()));
        snapshot.setGradeLevel(cleanRuleField(snapshot.getGradeLevel()));
        snapshot.setTopicRequirement(cleanRuleField(snapshot.getTopicRequirement()));
        snapshot.setBeautifyLevel(cleanRuleField(snapshot.getBeautifyLevel()));
        snapshot.setCustomRequirement(cleanRuleField(snapshot.getCustomRequirement()));
        snapshot.setDeductionDetail(cleanRuleField(snapshot.getDeductionDetail()));
        snapshot.setSnapshotLabel(cleanRuleField(snapshot.getSnapshotLabel()));

        if ((snapshot.getRuleName() == null || snapshot.getRuleName().isBlank())
                && snapshot.getSnapshotLabel() != null
                && !snapshot.getSnapshotLabel().isBlank()) {
            snapshot.setRuleName(snapshot.getSnapshotLabel());
        }
        if (snapshot.getRuleName() == null || snapshot.getRuleName().isBlank()) {
            snapshot.setRuleName("历史批改细则");
        }
        if (snapshot.getSnapshotLabel() == null || snapshot.getSnapshotLabel().isBlank()) {
            snapshot.setSnapshotLabel(snapshot.getRuleName());
        }
    }

    private String extractLegacyRuleName(String raw) {
        String ruleName = extractLegacyRuleField(raw, "本次选用的评分细则");
        if (ruleName != null && !ruleName.isBlank()) {
            return ruleName;
        }
        if (raw.contains("【细则规则层】") || raw.contains("请遵守以下公共规则")) {
            return "历史批改细则";
        }
        return cleanRuleDisplayName(raw.length() > 24 ? raw.substring(0, 24) + "..." : raw);
    }

    private String extractLegacyRuleField(String raw, String fieldName) {
        if (raw == null || raw.isBlank() || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(fieldName) + "[：:]\\s*(.+)");
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return cleanRuleField(matcher.group(1));
    }

    private String cleanRuleDisplayName(String text) {
        String cleaned = cleanRuleField(text);
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }
        if (cleaned.contains("\n")
                || cleaned.contains("你是一位经验丰富")
                || cleaned.contains("请遵守以下公共规则")
                || cleaned.contains("【细则规则层】")
                || cleaned.contains("作文内容：")) {
            return null;
        }
        return cleaned;
    }

    private String cleanRuleField(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replace("\r", "").trim();
        if (cleaned.isBlank()) {
            return null;
        }
        if ("null".equalsIgnoreCase(cleaned) || "undefined".equalsIgnoreCase(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ReviewRecordVO toReviewRecordVO(ReviewRecordEntity record) {
        ReviewRecordVO vo = ReviewRecordVO.builder()
                .reviewId(record.getReviewId())
                .essayId(record.getEssayId())
                .taskId(record.getTaskId())
                .ruleVersion(record.getRuleVersion())
                .reviewerType(record.getReviewerType())
                .reviewerId(record.getReviewerId())
                .modelVersion(record.getModelVersion())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .totalScore(record.getTotalScore())
                .status(record.getStatus())
                .errorMsg(record.getErrorMsg())
                .retryCount(record.getRetryCount())
                .createTime(record.getCreateTime())
                .build();
        tryFillEssayMeta(vo);
        return vo;
    }

    private void tryFillEssayMeta(ReviewRecordVO vo) {
        if (vo == null || vo.getEssayId() == null) {
            return;
        }
        EssayEntity essay = essayService.getById(vo.getEssayId());
        if (essay != null) {
            vo.setEssayTitle(essay.getTitle());
            vo.setSubmitType(essay.getSubmitType() != null ? essay.getSubmitType().getCode() : null);
        }
    }

    private List<ReviewRecordVO> enrichReviewRecordVos(List<ReviewRecordVO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<ReviewRecordEntity>> historyMap = records.stream()
                .map(ReviewRecordVO::getEssayId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        essayId -> essayId,
                        reviewMapper::selectReviewRecordsByEssayId
                ));

        for (ReviewRecordVO record : records) {
            ReviewRuleSnapshotDTO snapshot = parseRuleSnapshot(record.getRuleVersion());
            record.setRuleId(snapshot.getRuleId());
            record.setRuleName(snapshot.getRuleName() != null ? snapshot.getRuleName() : snapshot.getSnapshotLabel());
            record.setReviewType(snapshot.getReviewType());
            record.setGradeLevel(snapshot.getGradeLevel());

            List<ReviewRecordEntity> essayHistory = historyMap.getOrDefault(record.getEssayId(), Collections.emptyList());
            ReviewVersionMeta versionMeta = resolveReviewVersionMeta(essayHistory, record.getReviewId());
            record.setReviewVersion(versionMeta.reviewVersion());
            record.setLatestVersion(versionMeta.latestVersion());
        }
        return records;
    }

    private ReviewVersionMeta resolveReviewVersionMeta(Long essayId, Long reviewId) {
        if (essayId == null || reviewId == null) {
            return new ReviewVersionMeta(1, false);
        }
        return resolveReviewVersionMeta(reviewMapper.selectReviewRecordsByEssayId(essayId), reviewId);
    }

    private ReviewVersionMeta resolveReviewVersionMeta(List<ReviewRecordEntity> records, Long reviewId) {
        if (records == null || records.isEmpty() || reviewId == null) {
            return new ReviewVersionMeta(1, false);
        }
        int total = records.size();
        for (int index = 0; index < records.size(); index++) {
            ReviewRecordEntity item = records.get(index);
            if (Objects.equals(item.getReviewId(), reviewId)) {
                return new ReviewVersionMeta(total - index, index == 0);
            }
        }
        return new ReviewVersionMeta(total, false);
    }

    private record ReviewVersionMeta(Integer reviewVersion, Boolean latestVersion) {}

    private boolean hasReviewManagePrivilege() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority ->
                        "ROLE_TEACHER".equals(authority)
                                || "ROLE_ADMIN".equals(authority)
                                || "ROLE_2".equals(authority)
                                || "ROLE_3".equals(authority));
    }

    // =========================
    // 批改细则 + 评分维度配置（CRUD）
    // =========================

    public List<ReviewRuleEntity> listRules(Boolean enabledOnly) {
        if (Boolean.TRUE.equals(enabledOnly)) {
            return reviewMapper.selectEnabledRules();
        }
        return reviewMapper.selectAllRules();
    }

    public ReviewRuleEntity createRule(ReviewRuleEntity rule) {
        validateRule(rule, true);
        if (rule.getStatus() == null) {
            rule.setStatus(1);
        }
        reviewMapper.insertRule(rule);
        return reviewMapper.selectRuleById(rule.getRuleId());
    }

    public boolean updateRule(Long id, ReviewRuleEntity rule) {
        if (id == null) {
            throw new IllegalArgumentException("ruleId 不能为空");
        }
        rule.setRuleId(id);
        validateRule(rule, false);
        return reviewMapper.updateRuleById(rule) > 0;
    }

    public boolean updateRuleStatus(Long id, boolean enabled) {
        return reviewMapper.updateRuleStatus(id, enabled ? 1 : 0) > 0;
    }

    public boolean deleteRule(Long id) {
        return reviewMapper.deleteRuleLogic(id) > 0;
    }

    public List<ScoreDimensionEntity> listDimensions(Long ruleId, Boolean enabledOnly) {
        if (ruleId != null) {
            if (Boolean.TRUE.equals(enabledOnly)) {
                return reviewMapper.selectEnabledDimensionsByRuleId(ruleId);
            }
            return reviewMapper.selectDimensionsByRuleId(ruleId);
        }
        if (Boolean.TRUE.equals(enabledOnly)) {
            return reviewMapper.selectEnabledDimensions();
        }
        return reviewMapper.selectAllDimensions();
    }

    public ScoreDimensionEntity createDimension(ScoreDimensionEntity dim) {
        validateDimension(dim, true);
        if (dim.getStatus() == null) {
            dim.setStatus(1);
        }
        if (dim.getSortOrder() == null) {
            dim.setSortOrder(0);
        }
        reviewMapper.insertDimension(dim);
        return reviewMapper.selectDimensionById(dim.getDimensionId());
    }

    public boolean updateDimension(Long id, ScoreDimensionEntity dim) {
        if (id == null) {
            throw new IllegalArgumentException("dimensionId 不能为空");
        }
        dim.setDimensionId(id);
        validateDimension(dim, false);
        return reviewMapper.updateDimensionById(dim) > 0;
    }

    public boolean updateDimensionStatus(Long id, boolean enabled) {
        return reviewMapper.updateDimensionStatus(id, enabled ? 1 : 0) > 0;
    }

    public boolean deleteDimension(Long id) {
        return reviewMapper.deleteDimensionLogic(id) > 0;
    }

    private void validateRule(ReviewRuleEntity rule, boolean creating) {
        if (rule == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (creating && (rule.getRuleName() == null || rule.getRuleName().trim().isEmpty())) {
            throw new IllegalArgumentException("ruleName 不能为空");
        }
    }

    private void validateDimension(ScoreDimensionEntity dim, boolean creating) {
        if (dim == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (dim.getRuleId() != null && reviewMapper.selectRuleById(dim.getRuleId()) == null) {
            throw new IllegalArgumentException("关联的批改细则不存在");
        }
        if (creating && (dim.getDimensionName() == null || dim.getDimensionName().trim().isEmpty())) {
            throw new IllegalArgumentException("dimensionName 不能为空");
        }
        if (dim.getWeight() == null) {
            if (creating) {
                throw new IllegalArgumentException("weight 不能为空");
            }
        } else {
            if (dim.getWeight().compareTo(BigDecimal.ZERO) < 0 || dim.getWeight().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("weight 必须在 0 到 100 之间");
            }
        }
        if (creating && dim.getMaxScore() == null) {
            throw new IllegalArgumentException("maxScore 不能为空");
        }
    }

    private ReviewRuleEntity getReviewRuleForPrompt(Long ruleId) {
        if (ruleId != null) {
            ReviewRuleEntity selectedRule = reviewMapper.selectRuleById(ruleId);
            if (selectedRule == null) {
                throw new IllegalArgumentException("所选评分细则不存在");
            }
            if (!Objects.equals(selectedRule.getStatus(), 1)) {
                throw new IllegalArgumentException("所选评分细则未启用");
            }
            return selectedRule;
        }
        return reviewMapper.selectActiveRule();
    }

    private List<ScoreDimensionEntity> getPromptDimensions(ReviewRuleEntity selectedRule) {
        List<ScoreDimensionEntity> dimensions = selectedRule != null
                ? reviewMapper.selectEnabledDimensionsByRuleId(selectedRule.getRuleId())
                : Collections.emptyList();
        if (dimensions == null || dimensions.isEmpty()) {
            dimensions = reviewMapper.selectEnabledDimensions();
        }
        return dimensions == null ? Collections.emptyList() : dimensions;
    }

    // =========================
    // 内部：AI 调用 & 解析
    // =========================

    private ReviewResult callAi(String prompt) {
        log.info("使用 SpringAI 调用 DeepSeek API 评审作文");
        String rawContent = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        StructuredReviewPayloadDTO structuredPayload = extractStructuredReviewPayload(rawContent);
        String reviewContent = extractNarrativeReviewContent(rawContent);
        String score = structuredPayload != null && structuredPayload.getTotalScore() != null
                ? structuredPayload.getTotalScore().toPlainString()
                : extractScore(reviewContent);
        return ReviewResult.builder()
                .reviewContent(reviewContent)
                .score(score)
                .timestamp(System.currentTimeMillis())
                .rawContent(rawContent)
                .structuredPayload(structuredPayload)
                .build();
    }

    /**
     * 调用外部文本纠错服务，并按行将 diffs 落库到 text_correction 表
     * 接口：POST {correctServiceUrl}/correct，Body: { "text": "完整作文文本" }
     * 期望返回格式示例：
     * {
     *   "diffs": [
     *     { "lineNo": 1, "original": "原句", "corrected": "修改后", "comment": "说明" },
     *     ...
     *   ]
     * }
     */
    private void callTextCorrectionAndSave(Long reviewId, String essayContent) {
        if (reviewId == null || essayContent == null || essayContent.trim().isEmpty()) {
            log.warn("文本纠错跳过：reviewId={}, essayContent为空={}", reviewId, essayContent == null || essayContent.trim().isEmpty());
            return;
        }
        try {
            log.info("开始调用文本纠错服务，reviewId={}, 作文内容长度={}", reviewId, essayContent.length());
            
            String url = correctServiceUrl.endsWith("/")
                    ? correctServiceUrl + "correct"
                    : correctServiceUrl + "/correct";

            log.info("文本纠错服务URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", essayContent);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("正在发送请求到文本纠错服务...");
            ResponseEntity<Map> response = correctServiceRestTemplate.postForEntity(url, requestEntity, Map.class);
            log.info("文本纠错服务响应状态: {}", response.getStatusCode());
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("文本纠错服务返回非 200 或空响应，status={}, body={}", response.getStatusCode(), response.getBody());
                return;
            }

            log.info("文本纠错服务完整响应: {}", response.getBody());

            Object diffsObj = response.getBody().get("diffs");
            if (!(diffsObj instanceof List<?> diffsList) || diffsList.isEmpty()) {
                log.warn("文本纠错服务未返回 diffs 或为空，reviewId={}, 响应体: {}", reviewId, response.getBody());
                return;
            }

            log.info("文本纠错服务返回 {} 条纠错记录", diffsList.size());

            List<TextCorrectionEntity> entities = new ArrayList<>();
            for (Object o : diffsList) {
                if (!(o instanceof Map<?, ?> m)) {
                    log.warn("跳过非Map类型的纠错记录: {}", o);
                    continue;
                }

                log.debug("处理纠错记录，原始数据: {}", m);

                // 兼容多种字段命名：lineNo/startOffset/endOffset/errorType/suggestion/comment 等
                String original = m.get("original") != null ? m.get("original").toString() : null;
                if (original == null && m.get("original_text") != null) {
                    original = m.get("original_text").toString();
                }
                if (original == null && m.get("originalText") != null) {
                    original = m.get("originalText").toString();
                }
                
                String corrected = m.get("corrected") != null ? m.get("corrected").toString() : null;
                if (corrected == null && m.get("corrected_text") != null) {
                    corrected = m.get("corrected_text").toString();
                }
                if (corrected == null && m.get("correctedText") != null) {
                    corrected = m.get("correctedText").toString();
                }

                // 至少要有原文或改后文本才写入
                if (original == null && corrected == null) {
                    log.warn("跳过无效纠错记录（原文和改后文本都为空）: {}", m);
                    continue;
                }

                Integer startOffset = null;
                Object startObj = m.get("start");  // 优先使用 start
                if (startObj == null) startObj = m.get("startOffset");
                if (startObj == null) startObj = m.get("start_offset");
                if (startObj instanceof Number) {
                    startOffset = ((Number) startObj).intValue();
                } else if (startObj instanceof String s) {
                    try {
                        startOffset = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                }

                Integer endOffset = null;
                Object endObj = m.get("end");  // 优先使用 end
                if (endObj == null) endObj = m.get("endOffset");
                if (endObj == null) endObj = m.get("end_offset");
                if (endObj instanceof Number) {
                    endOffset = ((Number) endObj).intValue();
                } else if (endObj instanceof String s) {
                    try {
                        endOffset = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                }

                String errorType = null;
                if (m.get("type") != null) {  // 优先使用 type
                    errorType = m.get("type").toString();
                } else if (m.get("errorType") != null) {
                    errorType = m.get("errorType").toString();
                } else if (m.get("error_type") != null) {
                    errorType = m.get("error_type").toString();
                }

                String suggestion = null;
                if (m.get("suggestion") != null) {
                    suggestion = m.get("suggestion").toString();
                } else if (m.get("comment") != null) {
                    suggestion = m.get("comment").toString();
                }

                log.debug("解析结果: original={}, corrected={}, startOffset={}, endOffset={}, errorType={}, suggestion={}", 
                    original, corrected, startOffset, endOffset, errorType, suggestion);

                // 目前先固定版本号为 1，后续如果有多轮纠错再扩展
                TextCorrectionEntity entity = TextCorrectionEntity.builder()
                        .reviewId(reviewId)
                        .originalText(original)
                        .correctedText(corrected)
                        .startOffset(startOffset)
                        .endOffset(endOffset)
                        .errorType(errorType)
                        .suggestion(suggestion)
                        .revisionNo(1)
                        .build();
                entities.add(entity);
            }

            if (!entities.isEmpty()) {
                log.info("准备保存 {} 条文本纠错记录到数据库，reviewId={}", entities.size(), reviewId);
                
                // 打印第一条记录的详细信息用于调试
                if (!entities.isEmpty()) {
                    TextCorrectionEntity first = entities.get(0);
                    log.info("第一条纠错记录详情: reviewId={}, original={}, corrected={}, startOffset={}, endOffset={}, errorType={}, suggestion={}, revisionNo={}", 
                        first.getReviewId(), first.getOriginalText(), first.getCorrectedText(), 
                        first.getStartOffset(), first.getEndOffset(), first.getErrorType(), 
                        first.getSuggestion(), first.getRevisionNo());
                }
                
                int insertCount = reviewMapper.insertTextCorrections(entities);
                log.info("文本纠错结果已保存，reviewId={}，准备插入={}条，实际插入={}条", reviewId, entities.size(), insertCount);
                
                // 验证是否真的插入成功
                List<TextCorrectionEntity> saved = reviewMapper.selectTextCorrectionsByReviewId(reviewId);
                log.info("验证插入结果：从数据库查询到 {} 条纠错记录", saved.size());
            } else {
                log.warn("没有有效的文本纠错记录需要保存，reviewId={}", reviewId);
            }
        } catch (Exception e) {
            // 不影响主流程，只记录日志
            log.error("调用文本纠错服务失败，reviewId={}，err={}", reviewId, e.getMessage(), e);
            log.error("详细错误堆栈：", e);
        }
    }

    private BigDecimal parseTotalScore(String scoreStr) {
        if (scoreStr == null) {
            return null;
        }
        try {
            return new BigDecimal(scoreStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractScore(String content) {
        if (content == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?:【总分】|总分[：:]?|得分[：:]?|评分[：:]?)\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // 兜底：任意数字分
        Pattern fallback = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher m2 = fallback.matcher(content);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    /**
     * 保存各维度得分
     * @param reviewId 评审记录ID
     * @param reviewContent AI 返回的评审内容
     * @return 保存的得分列表（用于后续计算总分）
     */
    private List<ReviewScoreEntity> saveDimensionScoresIfAny(Long reviewId, ReviewResult reviewResult, ReviewRuleEntity selectedRule) {
        if (reviewResult == null) {
            return new ArrayList<>();
        }
        String reviewContent = reviewResult.getReviewContent();
        List<ScoreDimensionEntity> dims = getPromptDimensions(selectedRule);
        if (dims == null || dims.isEmpty()) {
            log.warn("未配置评分维度，无法保存各维度得分。建议在 score_dimension 表中配置维度。");
            return new ArrayList<>();
        }
        Map<String, StructuredScoreItemDTO> structuredScoreMap = buildStructuredScoreMap(reviewResult.getStructuredPayload());
        List<ReviewScoreEntity> list = new ArrayList<>();
        for (ScoreDimensionEntity d : dims) {
            BigDecimal score = null;
            StructuredScoreItemDTO structuredItem = structuredScoreMap.get(normalizeDimensionKey(d.getDimensionName()));
            if (structuredItem != null) {
                score = structuredItem.getScore();
            }
            if (score == null) {
                score = extractDimensionScore(reviewContent, d.getDimensionName());
            }
            if (score != null) {
                list.add(ReviewScoreEntity.builder()
                        .reviewId(reviewId)
                        .dimensionId(d.getDimensionId())
                        .weightSnapshot(d.getWeight())
                        .score(score)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            reviewMapper.insertReviewScores(list);
        }
        return list;
    }

    private Map<String, StructuredScoreItemDTO> buildStructuredScoreMap(StructuredReviewPayloadDTO structuredPayload) {
        if (structuredPayload == null || structuredPayload.getItems() == null || structuredPayload.getItems().isEmpty()) {
            return Collections.emptyMap();
        }
        return structuredPayload.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getDimensionName() != null && !item.getDimensionName().isBlank())
                .collect(Collectors.toMap(
                        item -> normalizeDimensionKey(item.getDimensionName()),
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private String normalizeDimensionKey(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
    }

    /**
     * 从 review_score 表中计算总分
     * 公式：总分 = Σ(score)（同一 review_id 下的所有 score 字段求和）
     * 
     * 说明：
     * - 从数据库中查询同一 review_id 的所有 review_score 记录
     * - 将所有 score 字段直接求和
     * - 不乘以权重，直接求和
     * 
     * @param reviewId 批改记录ID
     * @return 计算出的总分，如果没有得分记录则返回 null
     */
    private BigDecimal calculateTotalScoreFromSavedScores(Long reviewId) {
        if (reviewId == null) {
            return null;
        }

        // 从数据库查询同一 review_id 的所有得分记录
        List<ReviewScoreEntity> scores = reviewMapper.selectScoresByReviewId(reviewId);
        if (scores == null || scores.isEmpty()) {
            log.warn("review_id={} 没有得分记录，无法计算总分", reviewId);
            return null;
        }

        BigDecimal totalScore = BigDecimal.ZERO;  // Σ(score)

        for (ReviewScoreEntity score : scores) {
            BigDecimal scoreValue = score.getScore();
            if (scoreValue != null) {
                totalScore = totalScore.add(scoreValue);
                log.debug("维度得分：score={}, 累计总分={}", scoreValue, totalScore);
            }
        }

        // 保留2位小数，四舍五入
        totalScore = totalScore.setScale(2, java.math.RoundingMode.HALF_UP);

        log.debug("计算总分（从 review_score 表）：review_id={}, 得分记录数={}, 总分={}", 
                reviewId, scores.size(), totalScore);
        return totalScore;
    }

    private BigDecimal extractDimensionScore(String reviewContent, String dimensionName) {
        if (reviewContent == null || dimensionName == null) {
            return null;
        }
        String[] patterns = {
                String.format("【%s得分[：:](\\d+(?:\\.\\d+)?)\\s*分?】", Pattern.quote(dimensionName)),
                String.format("【%s[：:](\\d+(?:\\.\\d+)?)\\s*分?】", Pattern.quote(dimensionName)),
                String.format("%s得分[：:](\\d+(?:\\.\\d+)?)\\s*分", Pattern.quote(dimensionName)),
                String.format("%s[：:](\\d+(?:\\.\\d+)?)\\s*分", Pattern.quote(dimensionName))
        };
        for (String p : patterns) {
            Matcher m = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(reviewContent);
            if (m.find()) {
                try {
                    return new BigDecimal(m.group(1));
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    private void saveComments(Long reviewId, String reviewContent) {
        if (reviewContent == null || reviewContent.trim().isEmpty()) {
            return;
        }
        List<ReviewCommentEntity> comments = parseComments(reviewContent).stream()
                .peek(c -> c.setReviewId(reviewId))
                .toList();
        if (!comments.isEmpty()) {
            reviewMapper.insertReviewComments(comments);
        }
    }

    /**
     * 保存评论并填充位置信息
     * 对于修改意见(commentType=3)和改进建议(commentType=2)，尝试从作文内容中查找对应位置
     */
    private void saveCommentsWithPosition(Long reviewId, String reviewContent, String essayContent) {
        if (reviewContent == null || reviewContent.trim().isEmpty()) {
            log.warn("保存评论跳过：reviewContent 为空，reviewId={}", reviewId);
            return;
        }
        
        log.info("开始保存评论并填充位置信息，reviewId={}, 作文内容长度={}", reviewId, essayContent != null ? essayContent.length() : 0);
        
        List<ReviewCommentEntity> comments = parseComments(reviewContent);
        log.info("解析出 {} 条评论", comments.size());
        
        // 为每个评论填充位置信息
        for (ReviewCommentEntity comment : comments) {
            comment.setReviewId(reviewId);
            
            log.debug("处理评论：commentType={}, content长度={}", comment.getCommentType(), 
                comment.getContent() != null ? comment.getContent().length() : 0);
            
            // 总评(1)不需要位置信息，直接跳过
            if (comment.getCommentType() == 1) {
                log.debug("跳过总评，不需要位置信息");
                continue;
            }
            
            // 对于修改意见(3)和改进建议(2)，尝试提取原文并查找位置
            try {
                if (comment.getCommentType() == 3) {
                    log.debug("开始提取修改意见的位置信息");
                    // 修改意见：尝试解析 "原句 -> 修改后" 格式
                    extractPositionFromRevision(comment, essayContent);
                } else if (comment.getCommentType() == 2) {
                    log.debug("开始提取改进建议的位置信息");
                    // 改进建议：尝试从建议内容中提取引用的原文
                    extractPositionFromSuggestion(comment, essayContent);
                }
                
                if (comment.getStartOffset() != null) {
                    log.info("成功填充位置信息：commentType={}, startOffset={}, endOffset={}, relatedText={}", 
                        comment.getCommentType(), comment.getStartOffset(), comment.getEndOffset(), 
                        comment.getRelatedText() != null ? comment.getRelatedText().substring(0, Math.min(20, comment.getRelatedText().length())) : null);
                }
            } catch (Exception e) {
                log.error("提取位置信息失败：commentType={}, error={}", comment.getCommentType(), e.getMessage(), e);
            }
        }
        
        if (!comments.isEmpty()) {
            reviewMapper.insertReviewComments(comments);
            log.info("保存评论完成，reviewId={}, 评论数={}", reviewId, comments.size());
        }
    }

    /**
     * 从修改意见中提取位置信息
     * 解析格式：原句 -> 修改后、原句→修改后、"原句"修改为"修改后"等
     */
    private void extractPositionFromRevision(ReviewCommentEntity comment, String essayContent) {
        if (comment.getContent() == null || essayContent == null) {
            log.debug("跳过位置提取：content 或 essayContent 为空");
            return;
        }
        
        String content = comment.getContent();
        log.info("========== 开始提取修改意见位置 ==========");
        log.info("修改意见内容（前200字符）：{}", content.substring(0, Math.min(200, content.length())));
        log.info("作文内容长度：{}", essayContent.length());
        
        // 尝试多种格式提取原文
        String[][] patterns = {
            // 格式1: **原句**：xxx **修改后**：yyy
            {"\\*\\*原句\\*\\*[\\uff1a:](.*?)\\s*\\*\\*修改后\\*\\*", "格式1"},
            // 格式2: 原句：xxx 修改后：yyy
            {"原句[\\uff1a:](.*?)(?:修改后|修改为|改为)[\\uff1a:]", "格式2"},
            // 格式3: "xxx"修改为"yyy"
            {"\\u201c([^\\u201d]{3,})\\u201d\\s*(?:修改为|改为)\\s*\\u201c", "格式3"},
            // 格式4: 将"xxx"改为"yyy"
            {"将\\u201c([^\\u201d]{3,})\\u201d(?:改为|修改为)", "格式4"},
            // 格式5: xxx -> yyy 或 xxx→yyy (修复字符类范围错误)
            {"([^\\n\\u2192\\->]{5,})\\s*[\\-\\u2192>]+\\s*", "格式5"},
            // 格式6: 1. xxx 修改后：yyy
            {"\\d+[\\u3001.\\uff0e)]\\s*([^\\n]{5,})\\s*(?:修改后|改为)[\\uff1a:]", "格式6"},
        };
        
        for (int i = 0; i < patterns.length; i++) {
            String patternStr = patterns[i][0];
            String patternDesc = patterns[i][1];
            
            try {
                log.debug("尝试正则模式 {}: {}", i + 1, patternDesc);
                Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(content);
                
                int matchCount = 0;
                while (matcher.find()) {
                    matchCount++;
                    String originalText = matcher.group(1).trim();
                    log.info("正则模式 {} 匹配成功（第{}个），提取到原文：{}", i + 1, matchCount, originalText);
                    
                    // 清理原文（去除序号、引号、星号等）
                    String cleanedText = originalText
                        .replaceAll("^\\d+[\\u3001.\\uff0e)]\\s*", "")  // 去除开头序号
                        .replaceAll("^[\"'\\u201c\\u201d\\u2018\\u2019\\*]+|[\"'\\u201c\\u201d\\u2018\\u2019\\*]+$", "")  // 去除引号和星号
                        .replaceAll("\\s+", "")  // 去除所有空格，提高匹配率
                        .trim();
                    
                    log.info("清理后的原文：{}", cleanedText);
                    
                    if (cleanedText.length() < 3) {
                        log.debug("原文太短（<3字符），跳过");
                        continue;
                    }
                    
                    // 在作文中查找原文位置（也去除空格后匹配）
                    String essayContentNoSpace = essayContent.replaceAll("\\s+", "");
                    int startOffset = essayContentNoSpace.indexOf(cleanedText);
                    
                    if (startOffset >= 0) {
                        // 找到了！需要转换回原始位置（考虑空格）
                        int actualOffset = findActualOffset(essayContent, cleanedText, startOffset);
                        comment.setStartOffset(actualOffset);
                        comment.setEndOffset(actualOffset + cleanedText.length());
                        comment.setRelatedText(cleanedText);
                        log.info("✓ 修改意见找到位置：原文={}, startOffset={}, endOffset={}", 
                            cleanedText.substring(0, Math.min(30, cleanedText.length())), 
                            actualOffset, actualOffset + cleanedText.length());
                        log.info("========== 提取成功 ==========");
                        return;
                    } else {
                        log.debug("在作文中未找到原文（去空格后）：{}", cleanedText.substring(0, Math.min(30, cleanedText.length())));
                        
                        // 尝试部分匹配（取前10个字符）
                        if (cleanedText.length() >= 10) {
                            String partial = cleanedText.substring(0, 10);
                            int partialOffset = essayContentNoSpace.indexOf(partial);
                            if (partialOffset >= 0) {
                                log.info("找到部分匹配（前10字符）：{}", partial);
                                int actualOffset = findActualOffset(essayContent, partial, partialOffset);
                                comment.setStartOffset(actualOffset);
                                comment.setEndOffset(actualOffset + partial.length());
                                comment.setRelatedText(partial);
                                log.info("✓ 修改意见找到部分位置：原文={}, startOffset={}, endOffset={}", 
                                    partial, actualOffset, actualOffset + partial.length());
                                log.info("========== 提取成功（部分匹配） ==========");
                                return;
                            }
                        }
                    }
                }
                
                if (matchCount > 0) {
                    log.debug("正则模式 {} 匹配了 {} 次，但都未在作文中找到", i + 1, matchCount);
                }
            } catch (Exception e) {
                log.error("正则模式 {} 执行失败：{}", i + 1, e.getMessage());
            }
        }
        
        log.warn("修改意见未找到位置：尝试了所有正则模式都失败");
        log.info("========== 提取失败 ==========");
    }

    /**
     * 找到实际的偏移量（考虑空格）
     */
    private int findActualOffset(String essayContent, String cleanedText, int noSpaceOffset) {
        int actualOffset = 0;
        int noSpaceCount = 0;
        
        for (int i = 0; i < essayContent.length(); i++) {
            char c = essayContent.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (noSpaceCount == noSpaceOffset) {
                    return actualOffset;
                }
                noSpaceCount++;
            }
            actualOffset++;
        }
        
        return 0;
    }

    /**
     * 从改进建议中提取位置信息
     * 尝试从建议内容中提取引用的原文片段
     */
    private void extractPositionFromSuggestion(ReviewCommentEntity comment, String essayContent) {
        if (comment.getContent() == null || essayContent == null) {
            log.debug("跳过位置提取：content 或 essayContent 为空");
            return;
        }
        
        String content = comment.getContent();
        log.debug("开始从改进建议中提取位置，content前100字符：{}", content.substring(0, Math.min(100, content.length())));
        
        try {
            // 尝试提取引号中的内容作为原文引用 (使用 Unicode 转义)
            Pattern quotePattern = Pattern.compile("[\"'\\u201c\\u201d\\u2018\\u2019]([^\"'\\u201c\\u201d\\u2018\\u2019]{5,})[\"'\\u201c\\u201d\\u2018\\u2019]");
            Matcher matcher = quotePattern.matcher(content);
            
            while (matcher.find()) {
                String quotedText = matcher.group(1).trim();
                log.debug("提取到引用文本：{}", quotedText);
                
                // 在作文中查找引用文本的位置
                int startOffset = essayContent.indexOf(quotedText);
                if (startOffset >= 0) {
                    comment.setStartOffset(startOffset);
                    comment.setEndOffset(startOffset + quotedText.length());
                    comment.setRelatedText(quotedText);
                    log.info("改进建议找到位置：引用文本={}, startOffset={}, endOffset={}", 
                        quotedText, startOffset, startOffset + quotedText.length());
                    return; // 找到第一个匹配就返回
                } else {
                    log.debug("在作文中未找到引用文本：{}", quotedText);
                }
            }
        } catch (Exception e) {
            log.error("提取改进建议位置失败：{}", e.getMessage(), e);
        }
        
        log.debug("改进建议未找到位置：content前50字符={}", content.substring(0, Math.min(50, content.length())));
    }

    private String buildReviewPrompt(String essayTitle, String essayContent, ReviewRuleEntity selectedRule) {
        List<ScoreDimensionEntity> dimensions = getPromptDimensions(selectedRule);
        StringBuilder prompt = new StringBuilder();
        appendPromptSystemRules(prompt);
        appendPromptRuleLayer(prompt, selectedRule);
        appendPromptEssayContext(prompt, essayTitle, essayContent);
        appendPromptDimensions(prompt, dimensions);
        appendPromptOutputFormat(prompt);
        return prompt.toString();
    }

    private void appendPromptSystemRules(StringBuilder prompt) {
        prompt.append("你是一位经验丰富、评价标准稳定的语文老师，请只执行“内容批改”任务。\n");
        prompt.append("请遵守以下公共规则：\n");
        prompt.append("1. 批改时保持专业、客观、鼓励性的语气，既指出问题，也肯定优点。\n");
        prompt.append("2. 重点评价立意、选材、结构、语言表达和完成度，不要把错别字纠正当作本次任务主体。\n");
        prompt.append("3. 所有评分必须严格围绕给定评分细则和评分项展开，不要自行改动评分项名称。\n");
        prompt.append("4. 每个评分项都要给出简短评价，并明确写出该项得分。\n");
        prompt.append("5. 总评、建议和修改意见都必须结合作文原文，避免空泛套话。\n\n");
    }

    private void appendPromptRuleLayer(StringBuilder prompt, ReviewRuleEntity selectedRule) {
        if (selectedRule == null) {
            prompt.append("【细则规则层】未指定专用批改细则，按系统默认通用作文标准执行。\n\n");
            return;
        }

        prompt.append("【细则规则层】\n");
        prompt.append("本次选用的评分细则：").append(selectedRule.getRuleName()).append("\n");
        if (selectedRule.getReviewType() != null && !selectedRule.getReviewType().isBlank()) {
            prompt.append("批改类型：").append(selectedRule.getReviewType()).append("\n");
        }
        if (selectedRule.getGradeLevel() != null && !selectedRule.getGradeLevel().isBlank()) {
            prompt.append("适用学段：").append(selectedRule.getGradeLevel()).append("\n");
        }
        if (selectedRule.getTopicRequirement() != null && !selectedRule.getTopicRequirement().isBlank()) {
            prompt.append("题干要求：").append(selectedRule.getTopicRequirement()).append("\n");
        }
        if (selectedRule.getBeautifyLevel() != null && !selectedRule.getBeautifyLevel().isBlank()) {
            prompt.append("内容润色强度参考：").append(selectedRule.getBeautifyLevel()).append("\n");
        }
        if (selectedRule.getCustomRequirement() != null && !selectedRule.getCustomRequirement().isBlank()) {
            prompt.append("自定义批改要求：").append(selectedRule.getCustomRequirement()).append("\n");
        }
        if (selectedRule.getDeductionDetail() != null && !selectedRule.getDeductionDetail().isBlank()) {
            prompt.append("扣分细则：").append(selectedRule.getDeductionDetail()).append("\n");
        }
        if (selectedRule.getPromptTemplate() != null && !selectedRule.getPromptTemplate().isBlank()) {
            prompt.append("细则专属批改提示：").append(selectedRule.getPromptTemplate()).append("\n");
        }
        prompt.append("\n");
    }

    private void appendPromptEssayContext(StringBuilder prompt, String essayTitle, String essayContent) {
        if (essayTitle != null && !essayTitle.trim().isEmpty()) {
            prompt.append("作文标题：").append(essayTitle).append("\n\n");
        }
        prompt.append("作文内容：\n").append(essayContent).append("\n\n");
    }

    private void appendPromptDimensions(StringBuilder prompt, List<ScoreDimensionEntity> dimensions) {
        prompt.append("【评分项层】\n");
        if (dimensions != null && !dimensions.isEmpty()) {
            prompt.append("请严格按以下评分项完成内容批改，并给出每项得分：\n");
            for (int i = 0; i < dimensions.size(); i++) {
                ScoreDimensionEntity dim = dimensions.get(i);
                String weightText = dim.getWeight() != null
                        ? String.format("%.2f", dim.getWeight())
                        : "0.00";
                prompt.append(String.format("%d. %s（满分%.2f分，权重%s%%）：\n",
                        i + 1, dim.getDimensionName(), dim.getMaxScore(), weightText));
                if (dim.getDescription() != null && !dim.getDescription().isBlank()) {
                    prompt.append("   评分说明：").append(dim.getDescription()).append("\n");
                }
            }
            prompt.append("\n请在每个评分项评价后，明确标注得分，格式为：【评分项名称得分：XX分】。\n\n");
        } else {
            prompt.append("当前没有配置到专用评分项，请按通用作文标准完成内容批改：\n");
            prompt.append("1. 内容评价：评价文章的主题、立意、选材是否恰当【内容评价得分：XX分】\n");
            prompt.append("2. 结构分析：分析文章的开头、中间、结尾是否合理，段落安排是否清晰【结构分析得分：XX分】\n");
            prompt.append("3. 语言表达：检查是否有错别字、语法错误、标点符号使用是否正确【语言表达得分：XX分】\n");
            prompt.append("4. 修辞手法：识别文章中使用的修辞手法，评价其运用是否恰当【修辞手法得分：XX分】\n\n");
        }
    }

    private void appendPromptOutputFormat(StringBuilder prompt) {
        prompt.append("【固定输出层】\n");
        prompt.append("请严格按下面固定格式输出，必须包含以下全部标记，标记名称不要改动：\n");
        prompt.append("【评分JSON】\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"items\": [\n");
        prompt.append("    {\"dimensionName\": \"评分项名称\", \"score\": 0, \"comment\": \"该项简评\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"totalScore\": 0\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("【评语输出】\n");
        prompt.append("【总评】（对整篇作文的综合评价）\n");
        prompt.append("【改进建议】（给出3-8条可执行建议，分点列出）\n");
        prompt.append("【修改意见】（给出3-8条“原句 -> 修改后”式的具体修改，尽量从作文原文中摘句；如果原文无法摘取，也要给出可直接替换的修改示例）\n");
        prompt.append("【总分】XX（满分100）\n\n");
        prompt.append("注意：\n");
        prompt.append("1. 评分JSON 必须是合法 JSON，items 中的 dimensionName 必须与给定评分项名称完全一致。\n");
        prompt.append("2. totalScore 必须等于各评分项 score 之和。\n");
        prompt.append("3. 【评语输出】部分必须继续使用固定标签，方便系统解析。\n");
        prompt.append("4. 不要遗漏任何一个输出板块。\n");
        prompt.append("5. 不要只输出符号、空话或句号，内容必须完整可读。\n");
        prompt.append("6. 不要输出与本次内容批改无关的解释性前言。\n");
    }

    private List<ReviewCommentEntity> parseComments(String reviewContent) {
        final int MIN_CONTENT_LENGTH = 10;
        final String full = reviewContent.trim();
        List<ReviewCommentEntity> comments = new ArrayList<>();

        String summary = extractBetweenMarkers(full, "【总评】", List.of("【改进建议】", "【修改意见】", "【总分】"));
        if (summary != null && summary.trim().length() >= MIN_CONTENT_LENGTH) {
            comments.add(ReviewCommentEntity.builder()
                    .commentType(1)
                    .content(summary.trim())
                    .build());
        } else {
            // 兜底：没有明确总评标签时，才退回整段内容，避免前端完全没有总评可展示
            comments.add(ReviewCommentEntity.builder()
                    .commentType(1)
                    .content(full)
                    .build());
        }

        String suggestion = extractBetweenMarkers(full, "【改进建议】", List.of("【修改意见】", "【总分】", "【总评】"));
        if (suggestion != null && suggestion.trim().length() >= MIN_CONTENT_LENGTH) {
            comments.add(ReviewCommentEntity.builder().commentType(2).content(suggestion.trim()).build());
        }

        String revision = extractBetweenMarkers(full, "【修改意见】", List.of("【总分】", "【改进建议】", "【总评】"));
        if (revision != null && revision.trim().length() >= MIN_CONTENT_LENGTH) {
            comments.add(ReviewCommentEntity.builder().commentType(3).content(revision.trim()).build());
        }

        // 兜底：关键词段落
        if (revision == null || revision.trim().length() < MIN_CONTENT_LENGTH) {
            Pattern revisionPattern = Pattern.compile(
                    "(?:修改意见|具体修改|修改建议|错误指正|病句修改|错别字)[：:：]?\\s*([\\s\\S]+?)(?=\\n\\s*(?:【|\\d+\\.|综合评分|总分|改进建议|提升建议|建议|$))",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
            Matcher m = revisionPattern.matcher(full);
            if (m.find()) {
                String txt = m.group(1).trim();
                if (txt.length() >= MIN_CONTENT_LENGTH) {
                    comments.add(ReviewCommentEntity.builder().commentType(3).content(txt).build());
                }
            }
        }

        if (suggestion == null || suggestion.trim().length() < MIN_CONTENT_LENGTH) {
            Pattern suggestionPattern = Pattern.compile(
                    "(?:改进建议|提升建议|建议)[：:：]?\\s*([\\s\\S]+?)(?=\\n\\s*(?:【|\\d+\\.|综合评分|总分|修改意见|具体修改|修改建议|错误指正|$))",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
            Matcher m = suggestionPattern.matcher(full);
            if (m.find()) {
                String txt = m.group(1).trim();
                if (txt.length() >= MIN_CONTENT_LENGTH) {
                    comments.add(ReviewCommentEntity.builder().commentType(2).content(txt).build());
                }
            }
        }

        // 去重（避免兜底重复插入）
        return comments.stream()
                .filter(c -> c.getContent() != null && !c.getContent().trim().isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(c -> c.getCommentType() + "::" + c.getContent(), c -> c, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())
                ));
    }

    private String extractBetweenMarkers(String text, String startMarker, List<String> endMarkers) {
        if (text == null || startMarker == null) {
            return null;
        }
        int start = text.indexOf(startMarker);
        if (start < 0) {
            return null;
        }
        start = start + startMarker.length();

        int end = text.length();
        if (endMarkers != null) {
            for (String endMarker : endMarkers) {
                if (endMarker == null) continue;
                int idx = text.indexOf(endMarker, start);
                if (idx >= 0 && idx < end) {
                    end = idx;
                }
            }
        }
        if (start >= end) {
            return null;
        }
        return text.substring(start, end).trim();
    }

    private StructuredReviewPayloadDTO extractStructuredReviewPayload(String rawContent) {
        String jsonText = extractBetweenMarkers(rawContent, "【评分JSON】", List.of("【评语输出】"));
        if (jsonText == null || jsonText.isBlank()) {
            return null;
        }
        jsonText = stripJsonFence(jsonText);
        try {
            StructuredReviewPayloadDTO payload = objectMapper.readValue(jsonText, StructuredReviewPayloadDTO.class);
            if (payload.getItems() == null) {
                payload.setItems(new ArrayList<>());
            }
            return payload;
        } catch (Exception e) {
            log.warn("解析结构化评分 JSON 失败，降级回文本提分，error={}", e.getMessage());
            return null;
        }
    }

    private String extractNarrativeReviewContent(String rawContent) {
        String narrative = extractBetweenMarkers(rawContent, "【评语输出】", null);
        if (narrative == null || narrative.isBlank()) {
            return rawContent == null ? "" : rawContent.trim();
        }
        return narrative.trim();
    }

    private String stripJsonFence(String rawText) {
        if (rawText == null) {
            return null;
        }
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    /**
     * 打印当前token的剩余有效期
     */
    private void printTokenRemainingValidity() {
        log.info("========== 开始检查Token剩余有效期 ==========");
        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("【Token状态】无法获取当前请求(RequestContextHolder.getRequestAttributes()返回null)，无法打印token剩余有效期");
                log.info("========== Token检查结束 ==========");
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String headerName = jwtProperties.getHeaderName();
            String header = request.getHeader(headerName);
            
            log.info("【调试信息】请求头名称: {}, 请求头值是否存在: {}", headerName, header != null);
            if (request != null) {
                log.info("【调试信息】请求URI: {}", request.getRequestURI());
            }

            // 检查是否有token
            if (header == null || !header.startsWith(jwtProperties.getTokenPrefix())) {
                log.info("【Token状态】当前请求没有token，无法打印剩余有效期");
                if (header != null) {
                    log.info("【调试信息】请求头值(前50字符): {}...", header.substring(0, Math.min(50, header.length())));
                }
                log.info("========== Token检查结束 ==========");
                return;
            }

            // 提取token
            String token = header.substring(jwtProperties.getTokenPrefix().length()).trim();
            log.info("【调试信息】Token已提取，长度: {}", token.length());

            // 解析token获取过期时间
            Claims claims = jwtUtil.parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            // 计算剩余有效期（毫秒）
            long remainingMillis = expiration.getTime() - now.getTime();

            if (remainingMillis <= 0) {
                log.warn("【Token状态】当前token已过期！过期时间: {}", expiration);
            } else {
                // 转换为更易读的格式
                long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
                long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

                String remainingTime = String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);
                log.info("【Token剩余有效期】{}", remainingTime);
                log.info("【Token过期时间】{}", expiration);
                log.info("【Token剩余毫秒数】{}", remainingMillis);
            }
        } catch (Exception e) {
            log.error("【Token状态】打印token剩余有效期时发生错误: {}", e.getMessage(), e);
            log.error("异常堆栈:", e);
        }
        log.info("========== Token检查结束 ==========");
    }
}
