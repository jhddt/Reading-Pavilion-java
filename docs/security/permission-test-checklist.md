# 权限改造回归清单

## 1. 认证基础

- 未携带 Token 调用受保护接口，返回 `401`。
- 携带非法/过期 Token 调用受保护接口，返回 `401`。
- 合法 Token 调用本人接口（如 `/user/me`），返回 `200`。

## 2. 角色授权

- 学生/教师访问 `GET /user/selectAll` 返回 `403`。
- 管理员访问 `GET /user/selectAll` 返回 `200`。
- 学生/教师调用 `/review/rules` 的写接口（POST/PUT/PATCH/DELETE）返回 `403`。
- 管理员调用 `/review/rules` 与 `/review/dimensions` 写接口返回 `200`。
- 非管理员调用 `GET /file/url/path` 返回 `403`。

## 3. 对象归属（Owner Check）

- 用户A访问用户B的 `ocrId`、`essayId`、`fileId` 相关 OCR 接口，返回业务拒绝（当前实现为错误响应）。
- 用户A访问自己资源的 OCR 接口返回 `200`。
- 用户A调用 `GET /file/url/{fileId}` 访问用户B文件，返回拒绝。
- 用户A调用 `GET /file/download/{fileId}` 下载用户B文件，返回拒绝。

## 4. 兼容性

- 旧令牌 `ROLE_1/2/3` 与新语义角色 `ROLE_STUDENT/TEACHER/ADMIN` 都能通过鉴权链。
- 现有作文创建、提交、撤回、删除主流程不受影响。

## 5. 数据迁移验收

- 执行 `001_rbac_schema.sql` 后，4张RBAC表创建成功。
- 执行 `002_rbac_seed_and_migration.sql` 后：
  - `role`、`permission` 有初始化数据；
  - `user_role` 回填条数与有效用户数量匹配；
  - `role_permission` 映射存在且无重复（唯一索引校验）。
- 回滚脚本 `003_rbac_rollback.sql` 在测试库可成功执行。