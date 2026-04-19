package com.jhddt.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "批量批改任务")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("batch_review_task")
public class BatchReviewTaskEntity {

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    private Integer isDeleted;
}
