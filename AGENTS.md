# AGENTS.md (AI 协作指南 - 智能招聘系统)

## 核心角色与目标
你是本仓库的资深全栈工程师协作代理（兼容 Claude Code / Codex / Trae 等 AI 编码工具）。
### 核心目标
- 交付标准：产出**可运行、可演示、适配本科毕设答辩**的智能招聘系统代码与改动
- 约束优先级：编码规范 > 业务需求；冲突时以 `/docs/编码规范.md`、`/docs/前端页面.md` 为准
- 毕设定位：功能完整、逻辑清晰、界面克制，便于截图/论文阐述；不追求商业化平台的复杂度

## 前置必读（编码前强制执行）
开始编码前，必须通读/检索以下文件，确认实现约束：
### 需求与规范
- `/docs/需求文档.md`：角色、功能边界、推荐算法（余弦相似度、权重规则）
- `/docs/编码规范.md`：后端分层/ORM/鉴权/返回体等硬性约束
- `/docs/前端页面.md`：UI 风格、布局结构、交互规则（无限加载、克制风格）
- `/docs/库表设计.sql`：表结构与字段含义（改动需同步 SQL 与实体）

### 运行与配置
- `/README.md`：启动方式、内置账号说明（冲突时以代码/配置为准）
- `/scripts/dev.sh`：macOS/Linux 一键启动脚本
- `/scripts/dev.ps1`：Windows PowerShell 一键启动脚本
- `/scripts/dev.cmd`：Windows cmd 一键启动脚本
- `/backend/src/main/resources/application.yml`：后端数据库/邮箱/存储配置
- `/frontend/.env`：前端后端 API 地址

## 项目技术栈（Monorepo 结构）
| 目录       | 技术栈                                          |
|------------|-------------------------------------------------|
| `/backend` | Java 8 + Spring Boot + MyBatis-Plus + Sa-Token + MySQL |
| `/frontend`| Vue 3 + Vite + Element Plus + Axios             |
| `/docs`    | 需求与规范文档（细节以该目录文件为准）          |

## 快速启动与自检
### 启动步骤
1. 初始化数据库：执行 `/backend/sql/init.sql`（可选：`/backend/sql/seed_demo_data.sql`）
2. 后端启动：`cd backend && mvn -DskipTests spring-boot:run`（默认端口：8080）
3. 前端启动：`cd frontend && npm i && npm run dev`（默认端口：5173）
4. 一键启动：macOS/Linux 执行 `bash scripts/dev.sh`；Windows 执行 `scripts\dev.cmd`
5. 健康检查：GET 请求 `http://localhost:8080/api/health`

### 内置账号（以 BootstrapDataRunner 注入逻辑为准）
- 管理员：admin / 123456
- 求职者：user / 123456
- HR：hr / 123456

## 强约束规则
### 后端约束
#### 技术栈不可变
- 必须使用：Java 8、Spring Boot、MySQL、MyBatis-Plus、Sa-Token
- 禁止：替换 ORM 方案、更换鉴权体系

#### 分层与依赖
- 严格单向依赖：`controller -> service -> mapper`
- 禁止：controller 直接调用 mapper
- 强制：service 区分接口（interface）与实现（impl）

#### Controller 规则
- 仅允许使用 `@GetMapping` / `@PostMapping`
- 统一返回 `com.lanrecruitment.common.Response`
- DTO 入参必须通过 `@Validated + javax.validation` 校验
- 仅负责：入参校验、鉴权边界、调用 service、返回 Response（不写业务逻辑）

#### 实体规范
- 路径：`/backend/src/main/java/com/lanrecruitment/domain`
- 强制 Lombok 注解：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- 禁止：手写 getter/setter/构造方法

#### 数据库规则
- 禁止：联表查询、外键、MyBatis XML
- 强制：仅使用 MyBatis 注解 / MyBatis-Plus
- 复杂查询：通过业务代码分步查询 + 组装实现

#### 异常处理
- 必须使用全局异常处理器：`com.lanrecruitment.exception.GlobalExceptionHandler`
- 业务异常优先使用 `BizException`（保证错误码/信息一致性）

#### 认证鉴权
- 统一使用 Sa-Token 管理登录态、角色、权限
- 明确 USER/HR/ADMIN 角色的接口访问边界
- 鉴权逻辑：集中在 Controller 边界或统一配置（禁止散落在业务代码）

#### 工具类规范
- 通用逻辑：统一放入 `com.lanrecruitment.utils`
- 优先使用 Hutool 工具库
- 邮件能力：基于现有 `EmailClient` / `EmailProperties` 扩展

### 前端约束
#### 技术栈与风格
- 必须使用：Vue 3 + Vite + Element Plus + Axios
- 最高优先级：代码可读性（页面级开发为主，避免炫技/复杂抽象）
- 禁止：过度原生 DOM 操作、复杂动画/自定义 CSS（优先复用 Element Plus 组件）

#### UI 风格（毕设专用）
- 风格定位：现代克制、系统型/教学型（非商业化招聘平台风格）
- 配色规则：主色蓝青，背景白/浅灰；禁止大渐变、高饱和对比
- 复杂度：避免过度精致/复杂，符合本科毕设展示需求

#### 布局规范（全角色通用）
- 统一结构：所有角色共用一套页面布局，通过角色控制菜单/按钮/入口显示
- 推荐布局：
  - 顶部：固定导航（Logo/搜索/消息/个人入口/头像）
  - 主体：左右结构（左侧导航/筛选，右侧内容）

#### 列表交互
- 岗位列表：Feed 流展示
- 禁止：分页组件；强制使用“加载更多”/下拉加载

## 领域规则与算法
### 角色体系
- 核心角色：USER（求职者）、HR、ADMIN（管理员）
- 特殊规则：管理员账号仅系统初始化/配置生成，无独立注册入口

### 推荐算法（标签-权重）
- 核心逻辑：仅做排序，不做自动决策
- 计算方式：余弦相似度，向量为“标签-权重”：
  - 技能标签：基础权重 3 × 熟练度系数（精通3 / 熟悉2 / 了解1）
  - 行业/经验类：基础权重 1（无熟练度系数）

### 简历附件规则
- 支持格式：PDF / JPG / PNG
- 存储规则：与在线简历共存；上传后保证文件存储目录与访问 URL 一致（参考后端 `storage.base-dir` 配置）

## 代码修改与交付准则
1. 复用优先：先搜索同类 Controller/Service/DTO/VO，基于现有模式扩展
2. 最小变更：能在现有模块内完成的，不新建大规模结构
3. 前后端同步：新增接口时，保证路径、请求/响应字段、枚举值一致
4. 库表改动：先对照 `/docs/库表设计.sql` 与现有实体/Mapper
5. 技术栈兼容：禁止引入与仓库选型冲突的新框架/新范式
6. 可运行性：确保后端/前端能启动，关键流程可手动演示

## 核心代码定位（快速检索）
- 后端入口：`/backend/src/main/java/com/lanrecruitment/Application.java`
- 后端接口：`/backend/src/main/java/com/lanrecruitment/controller`
- 后端业务：`/backend/src/main/java/com/lanrecruitment/service` 及 `impl` 子目录
- 后端数据层：`/backend/src/main/java/com/lanrecruitment/mapper`
- 前端页面：`/frontend/src/pages`
- 前端请求封装：`/frontend/src/api/request.js`