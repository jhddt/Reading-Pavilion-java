ALTER TABLE `review_rule`
    ADD COLUMN `deduction_detail` TEXT NULL COMMENT '扣分细则' AFTER `custom_requirement`;
