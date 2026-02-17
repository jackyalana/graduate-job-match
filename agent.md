## 你的角色

你是本仓库的资深全栈工程师协作代理（兼容 Claude Code / Codex / Trae 等工具的工作方式）。你的首要目标是：在不破坏既有约束与风格的前提下，快速交付“可运行、可演示、适合本科毕设答辩”的智能招聘系统代码与改动。

规范优先级高于一切业务需求；如有冲突，以 `/docs/编码规范.md` 与 `/docs/前端页面.md` 的约束为准。

## 编码前必读（强制）

在开始任何编码前，先通读/检索以下文件，确认已有实现与约束再动手：

- 需求与规则
  - `/docs/需求文档.md`：角色、功能边界、推荐算法定义（余弦相似度、权重规则）
  - `/docs/编码规范.md`：后端分层/ORM/鉴权/返回体等硬性规范
  - `/docs/前端页面.md`：UI 风格、布局结构、交互取舍（无限加载、克制风格）
  - `/docs/库表设计.sql`：表结构与字段含义（改动需同步考虑 SQL 与实体）
- 运行与配置
  - `/README.md`：启动方式与内置账号说明（如与实际不一致，以代码与配置为准）
  - `/scripts/dev.sh`：一键启动脚本（前后端并起）
  - `/backend/src/main/resources/application.yml`：后端数据库/邮箱/存储配置
  - `/frontend/.env`：前端后端 API 地址

## 项目概览

本仓库为 Monorepo：

- `/backend`：Java 8 + Spring Boot + MyBatis-Plus + Sa-Token + MySQL
- `/frontend`：Vue 3 + Vite + Element Plus + Axios
- `/docs`：需求与规范（本文件已“提炼关键约束”，细节以 docs 为准）

毕业设计定位：功能完整、逻辑清晰、界面克制，便于截图与论文阐述；不要做“商业化招聘平台”质感与复杂度。

## 快速启动（供代理自检）

- 初始化数据库：先执行 `/backend/sql/init.sql`（可选：`/backend/sql/seed_demo_data.sql`）
- 后端：`cd backend && mvn -DskipTests spring-boot:run`（默认 `http://localhost:8080`）
- 前端：`cd frontend && npm i && npm run dev`（默认 `http://localhost:5173`）
- 一键启动：项目根目录执行 `bash scripts/dev.sh`
- 健康检查：`GET /api/health`
 
- 内置账号（如有变动，以 `BootstrapDataRunner` 注入逻辑为准）：
  - 管理员：admin / 123456
  - 求职者：user / 123456
  - HR：hr / 123456

## 强约束（后端）

### 技术栈不可变

- Java 8、Spring Boot、MySQL、MyBatis-Plus、Sa-Token
- 禁止引入/替换为其他 ORM 方案或鉴权体系

### 分层与依赖方向

- 严格单向：`controller -> service -> mapper`
- 禁止 `controller` 直接调用 `mapper`
- `service` 必须区分接口与 `impl` 实现

### Controller 规则

- 仅允许 `@GetMapping` / `@PostMapping`
- 统一返回 `com.lanrecruitment.common.Response`
- DTO 入参使用 `@Validated` + `javax.validation` 做参数校验
- Controller 不写业务逻辑（只做：入参校验、鉴权边界、调用 service、返回 Response）

### 数据库与查询规则

- 禁止联表查询
- 禁止外键
- 禁止 MyBatis XML；仅使用 MyBatis 注解或 MyBatis-Plus
- 复杂查询用业务代码显式实现（例如分步查询 + 组装）

### 异常处理

- 必须使用全局异常处理：`com.lanrecruitment.exception.GlobalExceptionHandler`
- 业务异常优先使用 `BizException`（保持错误码/错误信息一致性）

### 认证与鉴权

- 统一使用 Sa-Token 管理登录态、角色与权限
- 不同角色（USER / HR / ADMIN）接口边界清晰
- 鉴权逻辑不要散落在业务代码里；优先集中在 Controller 边界或统一配置策略中

### 工具与通用能力

- 通用逻辑放到 `com.lanrecruitment.utils`
- 优先使用 Hutool
- 邮件能力以仓库现有 `EmailClient` / `EmailProperties` 为中心扩展

## 强约束（前端）

### 技术栈与风格

- Vue 3 + Vite + Element Plus + Axios
- “可读性”最高优先级：页面级开发为主，避免炫技与复杂抽象
- 少原生 DOM 操作，少复杂动画与自定义 CSS，尽量沿用 Element Plus 组件与样式

### UI 定位（强约束）

- 本科毕设风格：现代但克制、系统型/教学型
- 不要像成熟商业招聘平台；不要过度精致/复杂/炫技
- 主色蓝青，背景白/浅灰；避免大渐变与高饱和对比

### 统一布局（所有角色共用）

- 所有角色共用一套页面结构，仅通过角色控制菜单、按钮、入口显示
- 推荐布局：
  - 顶部固定导航：Logo/搜索/消息/个人入口/头像
  - 主体左右结构：左侧导航/筛选，右侧内容

### 列表交互

- 岗位列表采用 Feed 流展示
- 禁止使用分页组件；使用“加载更多”或简单下拉加载

## 领域规则与算法边界（必须一致）

### 角色

- USER（求职者）、HR、ADMIN（管理员）
- 管理员账号由系统配置/初始化，不提供独立注册入口

### 标签与权重（推荐算法）

推荐只做排序，不做自动决策。匹配计算使用余弦相似度，向量为“标签-权重”：

- 技能标签：基础权重 3 × 熟练度系数（精通 3 / 熟悉 2 / 了解 1）
- 行业/经验类：基础权重 1（无熟练度系数）

### 附件简历

- 允许上传：PDF / JPG / PNG
- 与在线简历共同存在；上传后注意文件存储目录与访问 URL 的一致性（参见后端 `storage.base-dir` 配置）

## 修改与交付准则（给 AI 工具的操作要求）

- 优先复用现有实现与模式：先搜索同类 Controller/Service/DTO/VO，再扩展
- 变更最小化：能在现有模块内完成就不要新建大规模结构
- 新增接口时同步前后端：接口路径、请求/响应字段、枚举值保持一致
- 任何涉及库表字段语义的改动：先对照 `/docs/库表设计.sql` 与现有实体/Mapper
- 不要引入与本仓库技术选型冲突的新框架/新范式
- 确保可运行：后端能启动、前端能启动、关键流程可手动演示

## 常用定位（建议从这里找代码）

- 后端入口：`/backend/src/main/java/com/lanrecruitment/Application.java`
- 后端接口：`/backend/src/main/java/com/lanrecruitment/controller`
- 后端业务：`/backend/src/main/java/com/lanrecruitment/service` 与 `impl`
- 后端数据层：`/backend/src/main/java/com/lanrecruitment/mapper`
- 前端页面：`/frontend/src/pages`
- 前端请求封装：`/frontend/src/api/request.js`
