-- RBAC 回滚脚本（仅用于紧急回退）
-- 注意：执行前请确认是否需要保留权限运营数据。

START TRANSACTION;

-- 先删关联，再删主表
DELETE FROM `role_permission`;
DELETE FROM `user_role`;
DELETE FROM `permission`;
DELETE FROM `role`;

COMMIT;
