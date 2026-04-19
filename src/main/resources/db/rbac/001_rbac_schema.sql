-- RBAC 核心表结构（MySQL 8）
-- 执行前请先在测试库验证。

CREATE TABLE IF NOT EXISTS `role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码，如 ADMIN',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_status_deleted` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `permission` (
  `permission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码，如 user:manage',
  `permission_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
  `resource_type` VARCHAR(32) DEFAULT NULL COMMENT '资源类型：api/menu/button',
  `resource_path` VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '权限描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_perm_status_deleted` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_ur_role_deleted` (`role_id`, `is_deleted`),
  CONSTRAINT `fk_ur_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_ur_role_id` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS `role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_rp_perm_deleted` (`permission_id`, `is_deleted`),
  CONSTRAINT `fk_rp_role_id` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`),
  CONSTRAINT `fk_rp_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';
