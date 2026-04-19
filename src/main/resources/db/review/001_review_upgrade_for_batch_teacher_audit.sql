-- v4.1: 支持批量批改、教师修订、审计日志
-- 执行前请确认当前库为 MySQL 8+

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `review_record`
    ADD COLUMN `batch_task_id` BIGINT NULL COMMENT '批量任务ID' AFTER `task_id`,
    ADD COLUMN `source_review_id` BIGINT NULL COMMENT '来源评审ID(教师修订AI时使用)' AFTER `batch_task_id`;

ALTER TABLE `review_record`
    ADD INDEX `idx_rr_batch_task` (`batch_task_id`),
    ADD INDEX `idx_rr_source_review` (`source_review_id`);

CREATE TABLE IF NOT EXISTS `batch_review_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批任务ID',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `rule_id` BIGINT NULL COMMENT '批改规则ID',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0处理中 1完成 2完成(有失败)',
  `error_msg` VARCHAR(1000) NULL COMMENT '错误信息',
  `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` DATETIME NULL COMMENT '结束时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`task_id`),
  KEY `idx_brt_creator` (`creator_id`, `create_time`),
  KEY `idx_brt_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量批改任务表';

ALTER TABLE `batch_review_task`
    ADD CONSTRAINT `fk_brt_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) NULL COMMENT '用户名快照',
  `action` VARCHAR(100) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(50) NULL COMMENT '目标类型',
  `target_id` VARCHAR(100) NULL COMMENT '目标ID',
  `request_method` VARCHAR(10) NULL,
  `request_path` VARCHAR(255) NULL,
  `request_ip` VARCHAR(64) NULL,
  `user_agent` VARCHAR(500) NULL,
  `request_id` VARCHAR(64) NULL,
  `result_code` INT NULL,
  `success` TINYINT NOT NULL DEFAULT 1,
  `error_message` VARCHAR(1000) NULL,
  `before_data` LONGTEXT NULL,
  `after_data` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_audit_user_time` (`user_id`, `created_at`),
  KEY `idx_audit_action_time` (`action`, `created_at`),
  KEY `idx_audit_target` (`target_type`, `target_id`),
  KEY `idx_audit_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
