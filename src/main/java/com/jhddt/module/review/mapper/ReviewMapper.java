package com.jhddt.module.review.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jhddt.module.review.entity.ReviewCommentEntity;
import com.jhddt.module.review.entity.ReviewRecordEntity;
import com.jhddt.module.review.entity.ReviewScoreEntity;
import com.jhddt.module.review.entity.ScoreDimensionEntity;
import com.jhddt.module.review.entity.TextCorrectionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Review 模块统一 Mapper（按功能聚合，而非按表拆分）
 * <p>
 * 负责 review_record / review_score / review_comment / score_dimension 四张表的读写。
 * 具体 SQL 写在对应的 XML（ReviewMapper.xml）中，避免在注解中维护长 SQL。
 * </p>
 */
@Mapper
public interface ReviewMapper {

    // =========================
    // review_record
    // =========================

    int insertReviewRecord(ReviewRecordEntity record);

    int updateReviewRecordById(ReviewRecordEntity record);

    ReviewRecordEntity selectReviewRecordById(@Param("reviewId") Long reviewId);

    List<ReviewRecordEntity> selectReviewRecordsByEssayId(@Param("essayId") Long essayId);

    List<ReviewRecordEntity> selectReviewRecordsPage(Page<ReviewRecordEntity> page,
                                                     @Param("status") Integer status,
                                                     @Param("reviewerType") Integer reviewerType);

    // =========================
    // review_score
    // =========================

    int insertReviewScores(@Param("list") List<ReviewScoreEntity> list);

    List<ReviewScoreEntity> selectScoresByReviewId(@Param("reviewId") Long reviewId);

    // =========================
    // review_comment
    // =========================

    int insertReviewComments(@Param("list") List<ReviewCommentEntity> list);

    List<ReviewCommentEntity> selectCommentsByReviewId(@Param("reviewId") Long reviewId);

    // =========================
    // score_dimension
    // =========================

    List<ScoreDimensionEntity> selectAllDimensions();

    List<ScoreDimensionEntity> selectEnabledDimensions();

    ScoreDimensionEntity selectDimensionById(@Param("id") Long id);

    int insertDimension(ScoreDimensionEntity dim);

    int updateDimensionById(ScoreDimensionEntity dim);

    int updateDimensionStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteDimensionLogic(@Param("id") Long id);

    // =========================
    // text_correction
    // =========================

    int insertTextCorrections(@Param("list") List<TextCorrectionEntity> list);
}

