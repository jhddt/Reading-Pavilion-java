-- 初始化角色、权限，并把 user.role（1/2/3）迁移到 user_role
-- 依赖：001_rbac_schema.sql

START TRANSACTION;

-- 1) 初始化角色
INSERT INTO `role` (`role_code`, `role_name`, `description`)
VALUES
  ('STUDENT', '学生', '默认学生角色'),
  ('TEACHER', '教师', '教师角色'),
  ('ADMIN', '管理员', '系统管理员角色')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `description` = VALUES(`description`),
  `is_deleted` = 0,
  `status` = 1;

-- 2) 初始化权限点（首批）
INSERT INTO `permission` (`permission_code`, `permission_name`, `resource_type`, `resource_path`, `description`)
VALUES
  ('user:manage', '用户管理', 'api', '/user/**', '用户增删改查管理权限'),
  ('review:rule:read', '评审规则只读', 'api', '/review/rules', '查询批改细则'),
  ('review:rule:write', '评审规则写入', 'api', '/review/rules/**', '新增修改删除批改细则'),
  ('review:dimension:read', '评分维度只读', 'api', '/review/dimensions', '查询评分维度'),
  ('review:dimension:write', '评分维度写入', 'api', '/review/dimensions/**', '新增修改删除评分维度'),
  ('file:url:path', '按路径生成文件URL', 'api', '/file/url/path', '高风险接口，仅管理员'),
  ('ocr:read:owner', 'OCR查询（本人资源）', 'api', '/ocr/**', '查询本人OCR数据')
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `resource_type` = VALUES(`resource_type`),
  `resource_path` = VALUES(`resource_path`),
  `description` = VALUES(`description`),
  `is_deleted` = 0,
  `status` = 1;

-- 3) 初始化角色-权限映射
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM `role` r
JOIN `permission` p
  ON (
      (r.role_code = 'ADMIN' AND p.permission_code IN (
          'user:manage',
          'review:rule:read',
          'review:rule:write',
          'review:dimension:read',
          'review:dimension:write',
          'file:url:path',
          'ocr:read:owner'
      ))
      OR (r.role_code = 'TEACHER' AND p.permission_code IN (
          'review:rule:read',
          'review:rule:write',
          'review:dimension:read',
          'ocr:read:owner'
      ))
      OR (r.role_code = 'STUDENT' AND p.permission_code IN (
          'review:rule:read',
          'review:rule:write',
          'ocr:read:owner'
      ))
  )
ON DUPLICATE KEY UPDATE `is_deleted` = 0;

-- 4) 从 user.role 回填 user_role（双写过渡期）
INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.user_id, r.role_id
FROM `user` u
JOIN `role` r
  ON (
      (u.role = 1 AND r.role_code = 'STUDENT')
      OR (u.role = 2 AND r.role_code = 'TEACHER')
      OR (u.role = 3 AND r.role_code = 'ADMIN')
  )
WHERE u.is_deleted = 0
ON DUPLICATE KEY UPDATE `is_deleted` = 0;

COMMIT;

-- 双写过渡建议：
-- A. 保留 user.role 作为兼容字段，读优先 user_role，写同时更新 user.role 与 user_role
-- B. 过渡结束后，再移除业务代码对 user.role 的读取依赖
