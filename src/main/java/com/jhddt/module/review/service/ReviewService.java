package com.jhddt.module.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.module.essay.entity.EssayEntity;
import com.jhddt.module.essay.service.EssayService;
import com.jhddt.module.review.dto.ReviewResult;
import com.jhddt.module.review.entity.ReviewCommentEntity;
import com.jhddt.module.review.entity.ReviewRecordEntity;
import com.jhddt.module.review.entity.ReviewScoreEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.mapper.ReviewMapper;
import com.jhddt.module.review.vo.ReviewCommentVO;
import com.jhddt.module.review.vo.ReviewRecordDetailVO;
import com.jhddt.module.review.vo.ReviewScoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    // =========================
    // 评审主流程
    // =========================

    public ReviewRecordDetailVO reviewEssayAndSave(Long essayId, Long userId) {
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
                .status(1)
                .retryCount(0)
                .build();
        reviewMapper.insertReviewRecord(record);
        Long reviewId = record.getReviewId();

        try {
            // 5) 调用 AI
            ReviewResult aiResult = callAi(prompt);

            // 6) 保存各维度得分（先保存，用于计算总分）
            List<ReviewScoreEntity> savedScores = saveDimensionScoresIfAny(reviewId, aiResult.getReviewContent());

            // 7) 计算总分：从 review_score 表中同一 review_id 的所有 score 字段求和
            BigDecimal calculatedTotalScore = calculateTotalScoreFromSavedScores(reviewId);
            // 如果计算出总分就用计算的，否则用 AI 返回的总分作为兜底
            BigDecimal totalScore = calculatedTotalScore != null 
                    ? calculatedTotalScore 
                    : parseTotalScore(aiResult.getScore());

            // 8) 更新记录（SUCCESS）
            record.setReviewId(reviewId);
            record.setStatus(2);
            record.setEndTime(LocalDateTime.now());
            record.setTotalScore(totalScore);
            reviewMapper.updateReviewRecordById(record);

            // 9) 保存评论（总评/建议/修改意见）
            saveComments(reviewId, aiResult.getReviewContent());

            // 10) 返回详情
            return getReviewDetail(reviewId);
        } catch (Exception e) {
            // FAIL
            record.setReviewId(reviewId);
            record.setStatus(3);
            record.setEndTime(LocalDateTime.now());
            record.setErrorMsg(e.getMessage());
            reviewMapper.updateReviewRecordById(record);
            throw e;
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
        if (creating && dim.getWeight() == null) {
            throw new IllegalArgumentException("weight 不能为空");
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
                prompt.append(String.format("%d. %s（满分%.2f分，权重%.2f%%）：\n",
                        i + 1, dim.getDimensionName(), dim.getMaxScore(), dim.getWeight()));
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
}
