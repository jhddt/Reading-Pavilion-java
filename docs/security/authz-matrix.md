# 接口授权矩阵（V1）

本矩阵对应当前后端接口，采用“角色授权 + 资源归属校验（owner check）”双重控制。

## 用户模块


| 接口                     | 角色要求    | Owner Check | 说明     |
| ---------------------- | ------- | ----------- | ------ |
| `POST /user/login`     | 匿名      | 否           | 登录     |
| `POST /user/add`       | 匿名      | 否           | 注册     |
| `GET /user/me`         | 已登录     | 是（当前用户）     | 查询本人   |
| `PUT /user/me`         | 已登录     | 是（当前用户）     | 修改本人   |
| `POST /user/me/avatar` | 已登录     | 是（当前用户）     | 上传本人头像 |
| `GET /user/{id}`       | `ADMIN` | 否           | 用户管理查询 |
| `PUT /user/{id}`       | `ADMIN` | 否           | 用户管理修改 |
| `DELETE /user/{id}`    | `ADMIN` | 否           | 用户管理删除 |
| `GET /user/selectAll`  | `ADMIN` | 否           | 用户管理列表 |


## 评审模块


| 接口                                                       | 角色要求    | Owner Check | 说明         |
| -------------------------------------------------------- | ------- | ----------- | ---------- |
| `/review/essay/**`、`/review/record/**`、`/review/records` | 已登录     | 是（作文归属）     | 用户可操作自己的评审 |
| `POST /review/rules`                                     | 已登录     | 否           | 新增规则       |
| `PUT /review/rules/{id}`                                 | 已登录     | 否           | 修改规则       |
| `PATCH /review/rules/{id}/status`                        | 已登录     | 否           | 启停规则       |
| `DELETE /review/rules/{id}`                              | 已登录     | 否           | 删除规则       |
| `GET /review/rules`                                      | 已登录     | 否           | 查询规则（只读）   |
| `POST /review/dimensions`                                | `ADMIN` | 否           | 新增维度       |
| `PUT /review/dimensions/{id}`                            | `ADMIN` | 否           | 修改维度       |
| `PATCH /review/dimensions/{id}/status`                   | `ADMIN` | 否           | 启停维度       |
| `DELETE /review/dimensions/{id}`                         | `ADMIN` | 否           | 删除维度       |
| `GET /review/dimensions`                                 | 已登录     | 否           | 查询维度（只读）   |


## 文件与OCR模块


| 接口                                      | 角色要求    | Owner Check      | 说明          |
| --------------------------------------- | ------- | ---------------- | ----------- |
| `GET /file/download/{fileId}`           | 已登录     | 是（file.user_id）  | 下载本人文件      |
| `GET /file/url/{fileId}`                | 已登录     | 是（file.user_id）  | 获取本人文件临时链接  |
| `GET /file/essay/{essayId}`             | 已登录     | 是（essay.user_id） | 查询本人作文文件    |
| `GET /file/url/path`                    | `ADMIN` | 否                | 高风险接口，限制管理员 |
| `GET /ocr/{ocrId}`                      | 已登录     | 是（essay/file 归属） | OCR详情       |
| `GET /ocr/{ocrId}/detail`               | 已登录     | 是（essay/file 归属） | OCR详情+文本块   |
| `GET /ocr/{ocrId}/result-image`         | 已登录     | 是（essay/file 归属） | OCR对比图      |
| `GET /ocr/essay/{essayId}`              | 已登录     | 是（essay.user_id） | 按作文查询OCR    |
| `GET /ocr/file/{fileId}`                | 已登录     | 是（file.user_id）  | 按文件查询OCR    |
| `GET /ocr/essay/{essayId}/result-image` | 已登录     | 是（essay.user_id） | 按作文取OCR图    |


## 约定

- 角色语义名统一为：`ROLE_STUDENT`、`ROLE_TEACHER`、`ROLE_ADMIN`。
- 兼容历史令牌中的 `ROLE_1/2/3`，新旧角色映射并行。
- owner check 在 service/controller 双层都可做，但至少保证 service 层不可绕过。

