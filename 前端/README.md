# 阅读亭 · 作文批改前端（Vue3）

本目录是后端 `Reading-Pavilion-java` 的前端工程，使用 **Vue3 + Vite + Vue Router + Pinia + Axios** 实现作文管理、AI 批改、评分维度配置等功能。

## 1. 安装依赖

请先安装 Node.js（建议 18+），然后在本目录执行：

```bash
cd 前端
npm install
```

## 2. 启动前端

确保后端 Spring Boot 服务已在 `http://localhost:8080` 运行，然后在本目录执行：

```bash
npm run dev
```

浏览器访问 `http://localhost:5173` 即可。

> Vite 开发服务器已在 `vite.config.mts` 中配置了反向代理：
> - 前端所有调用以 `/api` 开头的请求，会被代理到后端 `http://localhost:8080`。

## 3. 主要技术栈

- **Vue3 Composition API**
- **Vue Router 4**：前端路由
- **Pinia**：用户登录状态（JWT Token）管理
- **Axios**：与后端接口交互（带请求/响应拦截器）
- **Vite**：开发/打包工具

## 4. 目录结构（核心部分）

```text
前端/
  ├─ index.html
  ├─ vite.config.mts
  ├─ package.json
  └─ src/
      ├─ main.js           # 入口文件
      ├─ App.vue
      ├─ styles.css        # 全局样式，布局 + 基础组件样式
      ├─ api/
      │   └─ http.js       # Axios 实例（带 Token 注入 & 401 处理）
      ├─ store/
      │   └─ auth.js       # 登录状态（Token、用户名）
      ├─ router/
      │   └─ index.js      # 路由配置 + 登录拦截
      └─ views/
          ├─ LoginView.vue         # 登录页
          ├─ MainLayout.vue        # 主布局（侧边菜单 + 顶部栏）
          ├─ EssayListView.vue     # 作文列表 + 发起批改
          ├─ EssayCreateTextView.vue # 文本作文创建
          ├─ ReviewListView.vue    # 批改记录列表
          ├─ ReviewDetailView.vue  # 批改详情（总分 + 维度得分 + 评语）
          └─ DimensionListView.vue # 评分维度增删改查

```

## 5. 与后端接口约定

前端已默认以下后端接口存在（与当前 Java 后端保持一致或稍作调整）：

- 登录：
  - `POST /user/login`  
    - 请求：`{ username, password }`
    - 响应：`{ code: 200, data: { token, username }, message }`
- 作文：
  - `POST /essay/text` 创建文本作文草稿
  - `GET /essay/list?page=&pageSize=` 查询当前用户作文分页列表（需在后端实现）
- 批改：
  - `POST /review/essay/{id}` 对指定作文发起 AI 批改
  - `GET /review/records?page=&pageSize=&status=&reviewerType=` 批改记录分页
  - `GET /review/record/{reviewId}` 批改详情（包含维度得分 + 评语）
- 评分维度：
  - `GET /review/dimensions?enabledOnly=` 查询评分维度列表
  - `POST /review/dimensions` 新增维度
  - `PATCH /review/dimensions/{id}/status?enabled=bool` 启用/禁用
  - `DELETE /review/dimensions/{id}` 删除（逻辑删除）

> 如后端返回结构与前端稍有不一致，可以在 `src/api/http.js` 或对应页面中做适配修改。

## 6. 鉴权与路由守卫

- 未登录用户访问受保护路由（除 `/login` 外的所有路由）时，会被自动重定向到 `/login`。
- 登录成功后，JWT Token 会被保存在 LocalStorage 和 Pinia 中，并自动附加到所有请求的 `Authorization: Bearer xxx` 头中。
- 后端返回 `401` 时，前端会自动清理 Token 并跳转回登录页。

