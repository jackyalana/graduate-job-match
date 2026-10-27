# 类似项目调研与复用决策

调研日期：2026 年 10 月 27 日。三个检索方向分别为校园就业推荐、招聘平台和通用后台底座，Exa 返回 18 条检索结果，其中有重复和售卖展示页；随后直接读取候选仓库文件核实。18 条结果不代表 18 个合格项目，也不是穷尽检索。

## 结论

存在可以大量复用基础业务的相近项目。首选 Lan-Recruitment，备选校园招聘推荐系统；不建议直接购买只有截图的源码，或把现成项目改名就当成独立开发成果。

这里的“大量复用”指账号、前端布局、表单、岗位与简历管理等模块，不是已测量的代码比例。没有本地启动和逐模块验收，不能承诺“80% 可用”或具体工期。

## 候选比较

| 候选与原始来源 | 技术与业务证据 | 授权核实 | 本课题用途 |
| --- | --- | --- | --- |
| [Lan-Recruitment](https://github.com/lanseqingling/lan-recruitment) | pom 为 Spring Boot 2.7.18、Java 8；前端 package.json 为 Vue 3、Vite、Element Plus；有标签余弦匹配及学生、HR、管理员页面 | 已读取 [MIT LICENSE](https://github.com/lanseqingling/lan-recruitment/blob/main/LICENSE) | 优先底座；匹配、冷启动、毕业生约束、评估需改造 |
| [校园招聘推荐系统](https://github.com/however-yir/campus-recruitment-recommendation-system) | Vue 2；当前 pom 为 Boot 2.7.18、Java 8；HybridJobRecommender 有内容、协同过滤分与热度融合 | 已读取 [MIT LICENSE](https://github.com/however-yir/campus-recruitment-recommendation-system/blob/main/LICENSE) | 备选或算法设计参考；不建议为节省算法工作而引入整套 Vue 2 工程 |
| [InternPilot](https://github.com/wan719/intern-pilot) | README 描述 Boot 3、Vue 3、AI 简历分析、岗位匹配、RAG 和投递管理；本次未追踪其 AI 调用链 | 已读取 [MIT LICENSE](https://github.com/wan719/intern-pilot/blob/main/LICENSE) | 适合以后扩展 AI 功能；与企业招聘审核闭环并不等同，初版引入会扩大范围 |
| [RuoYi-Vue3](https://github.com/yangzongzhuan/RuoYi-Vue3) | 官方 README 明确该仓库是前端，配套后端另在 RuoYi-Vue 或 RuoYi-Vue-fast | 已读取该前端仓库 MIT 文件，后端需单独核对 | 通用后台候选；并非完整招聘系统。若依的“岗位管理”是组织职务，不是招聘职位 |

售卖或展示类示例：[大学生就业需求分析](https://github.com/xunmaw001/analysis-system-for-employment-needs-of-young-college-students)、[graduate](https://github.com/small-bears/graduate)。检索页以项目介绍和交易联系为主，本次未验证到可直接复用的完整授权源码，因此不纳入推荐底座；这不等于断言它们没有代码。

## 首选底座的源码证据

锁定参考提交：`cd755d13443ea4b4c98be86e6c692238ed566d31`。GitHub 提交时间为 2026-03-08，提交说明为 Update README.md；本次读取的树未截断。实施时必须记录实际使用 SHA，不能仅写 main。

| 核实内容 | 固定提交源码 | 判断 |
| --- | --- | --- |
| 匹配算法 | [MatchServiceImpl.java](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/src/main/java/com/lanrecruitment/service/impl/MatchServiceImpl.java) | 读取简历和岗位标签，计算余弦分，读写内存或 Redis 缓存与 match_score；不是仅 README 声明 |
| 请求调用链 | [JobBrowseController.java](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/src/main/java/com/lanrecruitment/controller/JobBrowseController.java) 与 [JobBrowseServiceImpl.java](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/src/main/java/com/lanrecruitment/service/impl/JobBrowseServiceImpl.java) | recommend 接口调用 matchService.computeAndSave 并排序；有简历归属检查 |
| 角色与注册 | [AuthServiceImpl.java](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/src/main/java/com/lanrecruitment/service/impl/AuthServiceImpl.java) | USER、HR、ADMIN；禁止注册管理员；HR 登录检查审核状态，注册依赖邮件验证码 |
| 招聘业务 | [HrJobServiceImpl.java](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/src/main/java/com/lanrecruitment/service/impl/HrJobServiceImpl.java) | 岗位增改、本人岗位检查、投递查询、状态修改、附件下载；仍需端到端验证 |
| 数据与前端 | [init.sql](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/backend/sql/init.sql)、[package.json](https://github.com/lanseqingling/lan-recruitment/blob/cd755d13443ea4b4c98be86e6c692238ed566d31/frontend/package.json) | 有 10 张基础表、前端构建入口和 lockfile；树中有登录、简历、岗位、审核等页面 |

源码存在不能证明构建成功、权限完整或运行稳定。本次未拉取整个仓库、安装依赖、导入数据库或执行上游脚本。

## 已识别的改造点

1. 无简历时使用以 userId 为种子的 `order by rand(...)`，不能解释为个性化匹配。改为可复现的热度与新鲜度兜底，并明确提示资料不足。
2. 有简历时仅取最新 200 个岗位再计算相似度，会漏掉较早发布但更匹配的岗位。毕设规模采用对所有合格岗位批量打分，不逐岗位查库。
3. 原始评分主要是标签余弦相似度，没有本 SDD 要求的学历、毕业年份、城市、薪资和推荐理由。不能把随机或热门结果显示成“匹配度 95%”。
4. `job_apply` 唯一约束为 resume_id 与 job_id，同一学生用不同简历可产生同岗位多条记录。新规格按 user_id 与 job_id 限制一次投递。
5. `HrJobServiceImpl.remove` 会删除岗位关联投递；毕业设计的历史统计和投递追踪应保留快照，业务删除改为归档。
6. `updateApplyStatus` 检查枚举合法性，但未建立本 SDD 所需的前后状态转移校验。
7. 标签更新会清理原匹配缓存，但新算法还依赖画像、城市、薪资、算法权重与行为。必须重新定义版本与失效规则；并发算分的数据库唯一键冲突也要测试。
8. Java 8 与 Boot 2.7.18 是候选的实际版本，不作为新部署的“最新安全版本”推荐。先复现，再以独立变更处理兼容性升级，避免迁移与业务改造同时失去基线。

备选项目也不能照搬算法结论：[HybridJobRecommender](https://github.com/however-yir/campus-recruitment-recommendation-system/blob/main/src/main/java/com/utils/HybridJobRecommender.java) 的技能重叠分母是学生技能数，新增无关技能可能降低该子分；冷启动仍保留固定 CF 权重，学历判断在该工具类中作为评分而非硬过滤。这是工具类逻辑观察，不能据此推断所有控制器都缺少过滤。其 README 与 pom 的 Boot 版本表述不完全一致，应以锁定代码为准。

## 最大程度复用的实施方式

| 模块 | 策略 | 验收前提 |
| --- | --- | --- |
| Vue 布局、路由、登录表单、公共组件 | 保留并调整课题文案 | 前后端真实联通、没有旧路由绕过新规则 |
| 用户、岗位、简历、标签、公告 CRUD | 保留接口或加兼容适配层 | DTO 白名单、对象归属与后台权限通过测试 |
| 原表和演示数据 | 保留结构基础，使用增量迁移；演示数据重新生成 | 不复制私人信息、不在启动时破坏已有数据库 |
| 余弦工具 | 作为基线或可复用纯函数 | 数学边界测试、记录来源 |
| 画像、硬约束、匹配解释、冷启动、混合排序 | 按 SDD 实现 | 得分可重算，推荐原因来自实际特征 |
| 实验评估、报告、演示链路 | 自行实现 | 固定数据版本、种子、脚本及真实运行输出 |

不将两套系统拼接起来。备选项目只作为设计参考；确需复制片段时逐文件记录来源、提交和许可。MIT 文件允许修改与分发，并要求保留版权及许可文本；其他依赖、图片、字体与数据分别核对来源。学校是否允许以二次开发作为毕业设计，由学校要求决定，开源许可不替代学术要求。

## 路线选择与退出条件

默认路线 A：Lan-Recruitment 二次开发。M0 复现构建、业务链路与可修复性；记录实际复用清单，再进入 M1。

若源码或依赖获取失败、启动成本明显高于重建、权限缺陷牵动全系统，提交路线 B 的具体差异：使用 Spring Boot 与 Vue 3 独立实现 SDD，通用组件沿用开源库。审核后切换，不自行堆叠若依与招聘系统，也不因一次构建失败直接推倒重写。
