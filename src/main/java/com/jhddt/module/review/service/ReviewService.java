package com.jhddt.module.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.service.EssayService;
import com.jhddt.module.review.dto.ReviewResult;
import com.jhddt.module.review.dto.TextCorrectionDTO;
import com.jhddt.module.review.entity.ReviewCommentEntity;
import com.jhddt.module.review.entity.ReviewRecordEntity;
import com.jhddt.module.review.entity.ReviewScoreEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.entity.TextCorrectionEntity;
import com.jhddt.module.review.mapper.ReviewMapper;
import com.jhddt.module.review.vo.ReviewCommentVO;
import com.jhddt.module.review.vo.ReviewRecordDetailVO;
import com.jhddt.module.review.vo.ReviewScoreVO;
import com.jhddt.common.util.JwtUtil;
import com.jhddt.config.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Value("${correct.service.url:http://127.0.0.1:8001}")
    private String correctServiceUrl;

    // =========================
    // 评审主流程
    // =========================

    /**
     * 发起批改（异步处理）
     * 立即返回 reviewId 和 PROCESSING 状态，实际批改在后台异步执行
     */
    public ReviewRecordDetailVO reviewEssayAndSave(Long essayId, Long userId) {
        // 打印当前token的剩余有效期
        printTokenRemainingValidity();
        
        // 1) 查询作文并校验归属
        EssayEntity essay = essayService.getById(essayId);
        if (essay == null) {
            throw new IllegalArgumentException("作文不存在");
        }
        if (userId != null && essay.getUserId() != null && !Objects.equals(essay.getUserId(), userId)) {
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

        // 3) 构建提示词（并作为 rule_version 存档）
        String prompt = buildReviewPrompt(essay.getTitle(), essayContent);

        // 4) 先插入 review_record（PROCESSING）
        LocalDateTime startTime = LocalDateTime.now();
        ReviewRecordEntity record = ReviewRecordEntity.builder()
                .essayId(essayId)
                .reviewerType(0)
                .ruleVersion(prompt)
                .modelVersion("deepseek-chat")
                .startTime(startTime)
                .status(1) // PROCESSING
                .retryCount(0)
                .build();
        reviewMapper.insertReviewRecord(record);
        Long reviewId = record.getReviewId();

        // 5) 异步执行实际批改逻辑
        doReviewAsync(reviewId, essayId, essayContent, prompt, essay.getTitle());

        // 6) 立即返回，包含 reviewId 和 PROCESSING 状态
        ReviewRecordDetailVO vo = ReviewRecordDetailVO.builder()
                .reviewId(reviewId)
                .essayId(essayId)
                .essayTitle(essay.getTitle())
                .reviewerType(0)
                .modelVersion("deepseek-chat")
                .startTime(startTime)
                .status(1) // PROCESSING
                .build();
        return vo;
    }

    /**
     * 异步执行批改逻辑
     */
    @Async
    public void doReviewAsync(Long reviewId, Long essayId, String essayContent, String prompt, String essayTitle) {
        try {
            log.info("开始异步批改，reviewId={}, essayId={}", reviewId, essayId);

            // 1) 调用文本纠错服务，按行保存 diffs（不影响主流程，失败仅记录日志）
            callTextCorrectionAndSave(reviewId, essayContent);

            // 2) 调用 AI
            ReviewResult aiResult = callAi(prompt);

            // 3) 保存各维度得分（先保存，用于计算总分）
            List<ReviewScoreEntity> savedScores = saveDimensionScoresIfAny(reviewId, aiResult.getReviewContent());

            // 4) 计算总分：从 review_score 表中同一 review_id 的所有 score 字段求和
            BigDecimal calculatedTotalScore = calculateTotalScoreFromSavedScores(reviewId);
            // 如果计算出总分就用计算的，否则用 AI 返回的总分作为兜底
            BigDecimal totalScore = calculatedTotalScore != null 
                    ? calculatedTotalScore 
                    : parseTotalScore(aiResult.getScore());

            // 5) 更新记录（SUCCESS）
            ReviewRecordEntity record = ReviewRecordEntity.builder()
                    .reviewId(reviewId)
                    .status(2) // SUCCESS
                    .endTime(LocalDateTime.now())
                    .totalScore(totalScore)
                    .build();
            reviewMapper.updateReviewRecordById(record);

            // 6) 保存评论（总评/建议/修改意见）
            saveComments(reviewId, aiResult.getReviewContent());

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
        }
    }

    public ReviewRecordDetailVO getReviewDetail(Long reviewId) {
        ReviewRecordEntity record = reviewMapper.selectReviewRecordById(reviewId);
        if (record == null) {
            return null;
        }

        List<ReviewScoreEntity> scores = reviewMapper.selectScoresByReviewId(reviewId);
        List<ReviewCommentEntity> comments = reviewMapper.selectCommentsByReviewId(reviewId);

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
                        .createTime(c.getCreateTime())
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

        return ReviewRecordDetailVO.builder()
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
                .scores(scoreVOs)
                .comments(commentVOs)
                .build();
    }

    public List<ReviewRecordEntity> listByEssayId(Long essayId) {
        return reviewMapper.selectReviewRecordsByEssayId(essayId);
    }

    public Page<ReviewRecordEntity> pageRecords(Integer page, Integer pageSize, Integer status, Integer reviewerType) {
        Page<ReviewRecordEntity> p = new Page<>(page, pageSize);
        List<ReviewRecordEntity> records = reviewMapper.selectReviewRecordsPage(p, status, reviewerType);
        p.setRecords(records);
        return p;
    }

    // =========================
    // 评分维度配置（CRUD）
    // =========================

    public List<ScoreDimensionEntity> listDimensions(Boolean enabledOnly) {
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

    private void validateDimension(ScoreDimensionEntity dim, boolean creating) {
        if (dim == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (creating && (dim.getDimensionName() == null || dim.getDimensionName().trim().isEmpty())) {
            throw new IllegalArgumentException("dimensionName 不能为空");
        }
        if (dim.getWeight() == null) {
            if (creating) {
                throw new IllegalArgumentException("weight 不能为空");
            }
        } else {
            // 权重范围限制在 0-100（表示 0%-100%）
            if (dim.getWeight().compareTo(BigDecimal.ZERO) < 0 || dim.getWeight().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("weight 必须在 0 到 100 之间");
            }
        }
        if (creating && dim.getMaxScore() == null) {
            throw new IllegalArgumentException("maxScore 不能为空");
        }
    }

    // =========================
    // 内部：AI 调用 & 解析
    // =========================

    private ReviewResult callAi(String prompt) {
        log.info("使用 SpringAI 调用 DeepSeek API 评审作文");
        String reviewContent = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        String score = extractScore(reviewContent);
        return ReviewResult.builder()
                .reviewContent(reviewContent)
                .score(score)
                .timestamp(System.currentTimeMillis())
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
            return;
        }
        try {
            String url = correctServiceUrl.endsWith("/")
                    ? correctServiceUrl + "correct"
                    : correctServiceUrl + "/correct";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", essayContent);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("文本纠错服务返回非 200 或空响应，status={}", response.getStatusCode());
                return;
            }

            Object diffsObj = response.getBody().get("diffs");
            if (!(diffsObj instanceof List<?> diffsList) || diffsList.isEmpty()) {
                log.info("文本纠错服务未返回 diffs 或为空，reviewId={}", reviewId);
                return;
            }

            List<TextCorrectionEntity> entities = new ArrayList<>();
            for (Object o : diffsList) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }

                // 兼容多种字段命名：lineNo/startOffset/endOffset/errorType/suggestion/comment 等
                String original = m.get("original") != null ? m.get("original").toString() : null;
                if (original == null && m.get("original_text") != null) {
                    original = m.get("original_text").toString();
                }
                String corrected = m.get("corrected") != null ? m.get("corrected").toString() : null;
                if (corrected == null && m.get("corrected_text") != null) {
                    corrected = m.get("corrected_text").toString();
                }

                // 至少要有原文或改后文本才写入
                if (original == null && corrected == null) {
                    continue;
                }

                Integer startOffset = null;
                Object startObj = m.get("startOffset");
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
                Object endObj = m.get("endOffset");
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
                if (m.get("errorType") != null) {
                    errorType = m.get("errorType").toString();
                } else if (m.get("type") != null) {
                    errorType = m.get("type").toString();
                } else if (m.get("error_type") != null) {
                    errorType = m.get("error_type").toString();
                }

                String suggestion = null;
                if (m.get("suggestion") != null) {
                    suggestion = m.get("suggestion").toString();
                } else if (m.get("comment") != null) {
                    suggestion = m.get("comment").toString();
                }

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
                reviewMapper.insertTextCorrections(entities);
                log.info("文本纠错结果已保存，reviewId={}，行数={}", reviewId, entities.size());
            }
        } catch (Exception e) {
            // 不影响主流程，只记录日志
            log.error("调用文本纠错服务失败，reviewId={}，err={}", reviewId, e.getMessage(), e);
        }
    }

    /**
     * 测试文本纠错功能（不保存到数据库）
     * 调用外部文本纠错服务，返回纠错结果列表
     * 
     * @param text 待纠错的文本内容
     * @return 纠错结果列表
     */
    public List<TextCorrectionDTO> testTextCorrection(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        // 打印当前token的剩余有效期
        printTokenRemainingValidity();

        try {
            String url = correctServiceUrl.endsWith("/")
                    ? correctServiceUrl + "correct"
                    : correctServiceUrl + "/correct";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("文本纠错服务返回非 200 或空响应，status={}", response.getStatusCode());
                return new ArrayList<>();
            }

            // 调试：打印完整响应（用于排查字段名问题）
            log.debug("文本纠错服务完整响应: {}", response.getBody());

            Object diffsObj = response.getBody().get("diffs");
            if (!(diffsObj instanceof List<?> diffsList) || diffsList.isEmpty()) {
                log.info("文本纠错服务未返回 diffs 或为空，响应体: {}", response.getBody());
                return new ArrayList<>();
            }

            List<TextCorrectionDTO> result = new ArrayList<>();
            for (Object o : diffsList) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }

                // 调试：打印原始数据（仅第一条，避免日志过多）
                if (result.isEmpty()) {
                    log.debug("文本纠错服务返回的原始数据示例: {}", m);
                }

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

                // 至少要有原文或改后文本才返回
                if (original == null && corrected == null) {
                    continue;
                }

                // 解析起始位置：支持多种字段名
                Integer startOffset = null;
                Object startObj = m.get("startOffset");
                if (startObj == null) startObj = m.get("start_offset");
                if (startObj == null) startObj = m.get("start");
                if (startObj == null) startObj = m.get("startPos");
                if (startObj == null) startObj = m.get("start_pos");
                if (startObj instanceof Number) {
                    startOffset = ((Number) startObj).intValue();
                } else if (startObj instanceof String s && !s.trim().isEmpty()) {
                    try {
                        startOffset = Integer.parseInt(s.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 解析结束位置：支持多种字段名
                Integer endOffset = null;
                Object endObj = m.get("endOffset");
                if (endObj == null) endObj = m.get("end_offset");
                if (endObj == null) endObj = m.get("end");
                if (endObj == null) endObj = m.get("endPos");
                if (endObj == null) endObj = m.get("end_pos");
                if (endObj instanceof Number) {
                    endOffset = ((Number) endObj).intValue();
                } else if (endObj instanceof String s && !s.trim().isEmpty()) {
                    try {
                        endOffset = Integer.parseInt(s.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 解析错误类型：支持多种字段名
                String errorType = null;
                if (m.get("errorType") != null) {
                    errorType = m.get("errorType").toString();
                } else if (m.get("error_type") != null) {
                    errorType = m.get("error_type").toString();
                } else if (m.get("type") != null) {
                    errorType = m.get("type").toString();
                } else if (m.get("operation") != null) {
                    errorType = m.get("operation").toString();
                } else if (m.get("op") != null) {
                    errorType = m.get("op").toString();
                }

                // 解析建议：支持多种字段名
                String suggestion = null;
                if (m.get("suggestion") != null) {
                    suggestion = m.get("suggestion").toString();
                } else if (m.get("comment") != null) {
                    suggestion = m.get("comment").toString();
                } else if (m.get("message") != null) {
                    suggestion = m.get("message").toString();
                } else if (m.get("reason") != null) {
                    suggestion = m.get("reason").toString();
                } else if (m.get("description") != null) {
                    suggestion = m.get("description").toString();
                }

                TextCorrectionDTO dto = TextCorrectionDTO.builder()
                        .originalText(original)
                        .correctedText(corrected)
                        .startOffset(startOffset)
                        .endOffset(endOffset)
                        .errorType(errorType)
                        .suggestion(suggestion)
                        .build();
                result.add(dto);
            }

            log.info("文本纠错测试完成，返回 {} 条纠错结果", result.size());
            return result;
        } catch (Exception e) {
            log.error("调用文本纠错服务失败，err={}", e.getMessage(), e);
            throw new RuntimeException("文本纠错服务调用失败: " + e.getMessage(), e);
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
    private List<ReviewScoreEntity> saveDimensionScoresIfAny(Long reviewId, String reviewContent) {
        if (reviewContent == null || reviewContent.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<ScoreDimensionEntity> dims = reviewMapper.selectEnabledDimensions();
        if (dims == null || dims.isEmpty()) {
            log.warn("未配置评分维度，无法保存各维度得分。建议在 score_dimension 表中配置维度。");
            return new ArrayList<>();
        }
        List<ReviewScoreEntity> list = new ArrayList<>();
        for (ScoreDimensionEntity d : dims) {
            BigDecimal score = extractDimensionScore(reviewContent, d.getDimensionName());
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

    private String buildReviewPrompt(String essayTitle, String essayContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位经验丰富的语文老师，请对以下作文进行详细批改。\n");
        prompt.append("要求：专业、客观、鼓励性；指出问题同时肯定优点。\n\n");

        if (essayTitle != null && !essayTitle.trim().isEmpty()) {
            prompt.append("作文标题：").append(essayTitle).append("\n\n");
        }
        prompt.append("作文内容：\n").append(essayContent).append("\n\n");

        List<ScoreDimensionEntity> dimensions = reviewMapper.selectEnabledDimensions();
        if (dimensions != null && !dimensions.isEmpty()) {
            prompt.append("请按照以下评分维度进行批改，并为每个维度给出具体得分（满分见各维度说明）：\n");
            for (int i = 0; i < dimensions.size(); i++) {
                ScoreDimensionEntity dim = dimensions.get(i);
                String weightText = dim.getWeight() != null
                        ? String.format("%.2f", dim.getWeight())
                        : "0.00";
                prompt.append(String.format("%d. %s（满分%.2f分，权重%s%%）：\n",
                        i + 1, dim.getDimensionName(), dim.getMaxScore(), weightText));
            }
            prompt.append("\n请在每个维度的评价后，明确标注该维度的得分，格式为：【维度名称得分：XX分】\n");
        } else {
            prompt.append("请从以下几个方面进行批改：\n");
            prompt.append("1. 内容评价：评价文章的主题、立意、选材是否恰当【内容评价得分：XX分】\n");
            prompt.append("2. 结构分析：分析文章的开头、中间、结尾是否合理，段落安排是否清晰【结构分析得分：XX分】\n");
            prompt.append("3. 语言表达：检查是否有错别字、语法错误、标点符号使用是否正确【语言表达得分：XX分】\n");
            prompt.append("4. 修辞手法：识别文章中使用的修辞手法，评价其运用是否恰当【修辞手法得分：XX分】\n");
        }

        prompt.append("\n请严格按下面【固定格式】输出（务必包含每个标记，标记不要改名）：\n");
        prompt.append("【总评】（对整篇作文的综合评价）\n");
        prompt.append("【改进建议】（给出3-8条可执行建议，分点列出）\n");
        prompt.append("【修改意见】（给出3-8条“原句 -> 修改后”式的具体修改，尽量从作文原文中摘句；如果原文无法摘取，也要给出可直接替换的修改示例）\n");
        prompt.append("【总分】XX（满分100）\n\n");
        prompt.append("注意：不要只输出符号或句号，内容必须完整且可读。");
        return prompt.toString();
    }

    private List<ReviewCommentEntity> parseComments(String reviewContent) {
        final int MIN_CONTENT_LENGTH = 10;
        final String full = reviewContent.trim();
        List<ReviewCommentEntity> comments = new ArrayList<>();

        // 总评：始终保存完整内容（避免截断）
        comments.add(ReviewCommentEntity.builder()
                .commentType(1)
                .content(full)
                .build());

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
