# M0 复用矩阵

状态：待审核。按模块和证据决定，不计算或编造复用百分比。这里“可保留”只指相应片段；不意味着整个模块满足 SDD。

| FR / 模块 | 上游代码位置 | 本次证据 | 策略与后续边界 |
| --- | --- | --- | --- |
| FR01 账号 | controller/AuthController，service/impl/AuthServiceImpl，utils/PasswordUtil | 三角色密码登录、登出旧令牌拒绝、待审 HR 拒绝、禁止 ADMIN 注册 DTO | 保留 BCrypt 和角色值；改造 HTTP 错误、限流、禁用会话和 demo 正式区分；无有效邮件验证码注册未完整验证 |
| FR02 简历 CRUD | ResumeServiceImpl、UserResumeController、ResumeManage.vue | API 保存、标签、归属拒绝；浏览器新增后刷新保留 | 保留表单、本人归属检查及基本 CRUD 框架；新增画像/城市偏好/版本；归档、默认切换事务、快照需要改造 |
| FR03 岗位 | HrJobServiceImpl、JobManage.vue | HR 发待审岗位、管理员批准、浏览器闭环 | 保留列表/编辑 UI 和本人岗位校验；新增截止、资格、归档；标签修改也应重新审核，不能沿用 remove 物理删除 |
| FR04 浏览搜索 | JobBrowseServiceImpl、Home.vue | 审核前列表不见/详情拒绝，审核后浏览成功 | 保留搜索表单和公共卡片；统一 HR 状态/截止/资格过滤，结构化薪资和类别新增；原 city/type 文本需迁移 |
| FR05 匹配 | MatchServiceImpl | 同 Java 标签 1.0000，无交集 0.0000；真实写 match_score | 余弦作为标注来源的基线保留；主算法需按 SDD 新增 coverage、多维重归一、缺失值与解释。现有测试只覆盖缓存读，不是数学验收 |
| FR06 推荐 | JobBrowseServiceImpl.recommend，Home.vue | 无简历返回 null 分，实际随机 SQL；201 新岗位挤掉旧满分岗位 | 改造全部合格候选、冷启动、批次/版本；不保留最新 200 召回、N+1 算分、失效简历参与和已投递仍推荐 |
| FR07 收藏/隐藏 | 无对应实体/接口/UI | 静态目录核查 | 新增幂等状态、事件及 UI，未实施 |
| FR08 学生投递 | ApplyServiceImpl、job_apply | 一次成功/同简历重复拒绝；不同简历同人同岗成功两条 | 可复用入口与本人检查；改造唯一键、岗位锁、快照、事务与 HTTP 409；不能原样保留去重规则 |
| FR09 企业处理 | HrJobServiceImpl.listApplications/updateApplyStatus | 本人列表、HR2 跨岗拒绝、拒绝后能回到已投递 | 保留表格、标签展示和归属检查；改造状态机、事件、版本、历史简历/附件快照与排序解释 |
| FR10 审核/字典 | AdminAuditServiceImpl、TagServiceImpl、AdminUserServiceImpl | HR/岗位审批实际成功；学生无管理员权限 | 可保留审核/字典 UI；新增日志/理由，修复字典变更权重/缓存、禁用会话与物理删除；复杂边界未完整验证 |
| FR11 公告/统计 | NoticeServiceImpl、NoticeManage.vue、Layout.vue | 合成公告已预置；静态阅读公共消息入口 | 公告 CRUD 待专项验证后保留，统计新增；不以后台已有菜单当统计已实现 |
| FR12 评估 | 无 evaluation 模块 | 未执行离线推荐评估/性能实验 | 全部新增；M0 分数样例与并发探针不是精度或性能报告 |
| 公共布局/路由/请求 | Layout.vue、router/index.js、api/request.js | 三角色浏览器入口及联通 | 优先保留 Vue 3/布局/Element Plus；适配新契约、空态和 HTTP 状态；390px、失败重试未验收 |
| 文件存储下载 | FileStorageServiceImpl、WebMvcConfig、ResumeServiceImpl | 伪 PDF 被接受，匿名 URL HTTP 200 | UUID 文件名和路径检查可参考；私有存储、MIME/头/5MB 校验、快照引用必须改造；不能原样复用公开简历目录 |
| 会话/缓存 | SaTokenConfig、SaTokenDaoStoreConfig、MatchScoreCacheConfig、cache/* | memory 模式启动、重启重新登录；并发 3 次 500/1062 | 单实例 memory 路线可保留；缓存 key、原子失效、事务提交、upsert 必须改造；Redis 未实机验收 |
| 数据和脚本 | backend/sql/init.sql、seed_demo_data.sql、scripts/dev.* | 原 schema 在隔离库导入成功；原种子/启动脚本未执行 | 保留原结构作增量迁移输入；使用自建合成种子、明确 profile；旧脚本不得作为跳过测试证明 |

原样保留的业务实现范围尚需 M1 权限和迁移验收收紧。M0 未新增任何 SDD 核心业务代码；所有新 Python/Java 辅助程序均为复现、合成种子或证据工具，不冒充线上算法。
