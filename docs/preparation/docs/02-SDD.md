# 毕业生就业推荐与岗位匹配系统 SDD

版本：0.1。日期：2026 年 10 月 27 日。用途：本科毕业设计的软件设计和实施基线。状态：开发准备稿，待 M0 审核后冻结技术版本与上游映射。

本系统为毕业生提供岗位检索、画像与简历管理、可解释的岗位匹配、个性化推荐和投递跟踪；为企业提供岗位维护和已投递候选人排序；为管理员提供审核和统计。默认采用 Lan-Recruitment 二次开发，核心业务不依赖付费模型 API。

## 1 目标和范围

### 1.1 可验收目标

- 学生完成画像后，得到真实后端计算的匹配分、已匹配技能和待补齐技能。
- 无历史行为、画像不完整和候选不足时均有明确返回，不伪造推荐分或补入不合格岗位。
- 企业发布岗位，经管理员审核后可见；学生投递后，企业只能查看投向本账号岗位的简历。
- 学生看到投递状态时间线；状态流转、权限、算法结果具有测试与运行证据。
- 至少比较热门基线、纯内容匹配和混合推荐，按实际结果讨论优缺点。

### 1.2 必做和暂不做

P0：三个角色、账号登录、企业审核、岗位审核与搜索、结构化简历、求职偏好、标签字典、内容匹配、可解释推荐、收藏、投递状态、基础统计、评估脚本、可复现启动。

P1：简化协同过滤与混合排序、PDF 简历附件、审核与操作日志、实验展示页。P1 在最终答辩版本必须交付；“P1”表示实现顺序，不代表可在最终验收中跳过。

不纳入初版：真实招聘站爬虫、短信付费服务、即时聊天、视频面试、在线支付、多学校多租户、自动签约、复杂深度模型、RAG、LLM 决定录用。教师角色、就业去向登记和学校就业率报表属于后续扩展。

默认单企业单 HR 账号。企业候选排序仅对已投递人员开放，不提供全站学生简历搜索。平台录用状态不是实际就业确认，统计名称不得写成“学校就业率”。

## 2 角色与需求编号

沿用底座角色值 `USER / HR / ADMIN`，界面分别显示毕业生、企业、管理员；不要同时引入另一套 STUDENT 枚举。

| 编号 | 角色与需求 | 完成标准 |
| --- | --- | --- |
| FR01 | 注册、登录、退出、当前用户 | 禁止自选 ADMIN；密码使用 BCrypt；退出后令牌失效 |
| FR02 | 学生画像与简历 | 学校、专业、毕业年份、学历、技能熟练度、期望类别/城市/最低月薪；只能改本人资料 |
| FR03 | 企业准入与岗位 | 待审企业不能发布有效岗位；岗位增改后重新审核，支持截止时间和归档 |
| FR04 | 搜索和筛选 | 关键字、城市、岗位类别、薪资范围；仅展示审核通过、在招且未过期岗位 |
| FR05 | 匹配详情 | 输出 0 至 100 的匹配分或 null、各维度得分、缺失字段、匹配/缺失技能 |
| FR06 | 推荐列表 | 排除已投递、不感兴趣和不合格岗位；解释冷启动与候选不足 |
| FR07 | 收藏和不感兴趣 | 幂等操作；取消可恢复；收藏不默认从推荐中排除 |
| FR08 | 学生投递 | 选择本人有效简历；每人每岗位一次；保存简历和岗位快照 |
| FR09 | 企业筛选与处理 | 仅查询本 HR 岗位的投递；匹配排序与状态转移校验 |
| FR10 | 管理审核与字典 | 用户启停、HR 审核、岗位审核、标签/类别维护；记录操作人、时间、理由 |
| FR11 | 统计和公告 | 已审核在招岗位数、投递数、各状态人数、类别分布；公告 CRUD |
| FR12 | 算法评估与证据 | 同一候选集和时间切分下输出基线对比、样本数、冷启动结果和局限 |

权限必须在后端执行。学生 A 不能读写学生 B 的简历；HR A 不能读取 HR B 岗位的投递；管理员默认无批量导出完整学生简历权限。默认查询返回必要字段，账号对象中的 password 永不序列化。

## 3 架构与技术决策

采用单体后端和单 Vue 应用，按角色组织路由。浏览器通过 Vite 代理或 Nginx 访问 `/api`；Spring Boot 的 Controller 做参数和认证入口，Service 管理业务事务、授权与状态流转，Mapper 访问 MySQL，MatchingService 负责纯匹配计算，RecommendationService 负责候选、排序和日志。

| 层级 | 选择 |
| --- | --- |
| 基线复现 | 上游 Java 8、Spring Boot 2.7.18、MyBatis-Plus、Sa-Token，先原样验证 |
| 前端 | 保留 Vue 3、Vite、Element Plus、Vue Router、Axios 和现有 JS；不为统一语言强行改为 TypeScript |
| 数据 | MySQL 8 系列，具体补丁在 M0 固定；utf8mb4；数据库与服务时间统一 UTC，页面按 Asia/Shanghai 显示 |
| 会话与缓存 | 初版单实例采用底座内存实现，Redis 可选；重启后重新登录是已知行为，不是缺陷 |
| 测试 | 后端 JUnit、Spring 测试；关键数据库行为使用 MySQL；前端浏览器端到端测试 |
| 部署 | 本机开发与 Docker Compose 两种说明；数据库与上传目录持久化 |

M0 后新增 ADR 记录是否升级到 Java 17 和兼容的 Spring Boot 3 版本。默认先保留基线完成本地演示，不同时进行框架大版本迁移和业务重写。若学校或部署环境要求新版，则将迁移放在独立里程碑，并验证 Jakarta 包、Sa-Token、MyBatis starter 和测试兼容性。任何联网部署前重新检查依赖支持与漏洞，旧基线不自动获得部署通过结论。

目标目录保留上游 `backend/ frontend/ scripts/ docs/`；新增 `evaluation/`、`docs/evidence/`、`docs/adr/`、`THIRD_PARTY_NOTICES.md`。实施阶段在 `docs/BASELINE.md` 记录上游 URL、SHA、许可证、版本、启动命令、复用模块和独立改造模块。

## 4 数据设计

现有表采用增量迁移，不重新命名所有实体。以下为目标逻辑模型；实施 AI 必须给出可执行迁移 SQL、字段字典、索引和旧数据映射，不以本表代替实际 DDL。

| 表 | 关键字段和约束 |
| --- | --- |
| sys_user | 保留 id、username、password、email、role、status、audit_status；用户名、邮箱唯一；HR 归属以 id 而非 company_name 判断 |
| student_profile 新增 | user_id 唯一外键；major_code、education_rank、graduation_year、expected_category_id、salary_min、profile_version；学校名称仅展示，不参与评分 |
| student_city_preference 新增 | user_id、city_code，联合唯一；空集合表示未设置偏好 |
| resume | user_id、结构化项目经历、学历、版本 version、默认标识、状态；推荐使用本人有效默认简历；无默认时要求选择，不隐式使用作废简历 |
| tag / resume_tag / job_tag | 技能以标准 tag_id 关联；熟练度 1 至 3、权重为正；标签种类隔离，技能余弦不混入行业/经验标签；关联联合唯一 |
| job | hr_id；category_id、city_code、salary_min/max、salary_negotiable、education_min_rank、graduate_year_min/max、accept_fresh_graduate、deadline、version；保留展示字段；审核状态与招聘状态分离 |
| job_apply | user_id、resume_id、job_id；增加 UNIQUE(user_id,job_id)、status、version、resume_snapshot JSON、job_snapshot JSON、match_snapshot JSON；禁止业务级物理删除 |
| application_event 新增 | application_id、from_status、to_status、actor_id、reason、created_at；和状态更新同事务 |
| user_job_action 新增 | user_id、job_id、action_type FAVORITE/HIDE、active；三字段联合唯一；重复请求幂等 |
| behavior_event 新增 | user_id、job_id、event_type、event_time、dedupe_key 唯一；记录浏览/收藏/投递及取消，不保存电话或简历全文 |
| recommendation_batch 新增 | id、user_id、resume_id、profile_version、resume_version、algorithm_version、catalog_version、behavior_version、generated_at、snapshot JSON、fallback_reason；含有限候选与分数快照 |
| match_score | 原 score 作为余弦基线保留或迁移；新结果包含 algorithm_version、profile/resume/job/tag 版本和 breakdown；不可把旧余弦分当新总分 |
| audit_log 新增 | actor_id、resource_type、resource_id、action、reason、created_at；不写明文密码、token、附件内容 |
| notice / login_log | 保留基础结构；公告按发布时间和状态可见，登录日志脱敏 |

学历枚举：专科=1、本科=2、硕士=3、博士=4；岗位“不限”用 null。工资为人民币税前月薪整数元，仅比较月薪；面议值为 null，禁止字符串“8k-12k”参与数学比较。毕业年份为四位整数；不限制时上下界 null。实习日薪不混入月薪计算，初版仅覆盖全职应届岗位。

字段来源统一：学历、专业、毕业年份和求职偏好以 student_profile 为准，学校名称沿用 sys_user.school；技能与经历来自用户选择的有效简历。旧 resume.education 仅作迁移输入与历史展示，不能与 profile 同时成为两个评分来源。投递快照记录上述合并后的有效画像、简历和版本。

必要索引：job(audit_status,status,deadline)、job(city_code,category_id)、job(hr_id)；job_apply(user_id,created_at)、job_apply(job_id,status)；behavior_event(user_id,event_time)、behavior_event(job_id,event_time)；关联表的双向查询索引。迁移前清理重复投递时不得直接删历史：在隔离演示库验证归档映射并形成脚本报告。

业务完整性：投递时锁定岗位行并检查审核、在招、期限、HR 状态和本人简历；岗位关闭使用同一锁顺序，关闭后不得插入新投递。重复投递以数据库唯一键兜底返回 409。状态修改使用 version 条件更新；并发修改冲突返回 409。默认简历切换在锁定用户行的事务中完成。快照记录投递当时内容，后续改简历不改变历史投递。

## 5 业务流程与状态

企业注册为 PENDING，经管理员 APPROVED 后才能发布；REJECTED 可补材料重新提交。初版不自动发送真实邮件：demo profile 使用预置账号和本地演示注册，邮箱注明未验证；正式邮箱流程保留但默认禁用发送。普通 profile 不提供万能验证码，后台管理员只能通过安全初始化创建。

岗位审核状态为 PENDING、APPROVED、REJECTED；招聘状态为 OPEN、CLOSED、ARCHIVED。编辑岗位正文、待遇、技能或资格要求会增加 version 并回到 PENDING。关闭无需重新审核；重新开放必须仍满足审核和期限条件。过期岗位即使定时任务没执行也必须被查询与投递校验过滤。

投递状态：SUBMITTED → VIEWED → INTERVIEW → OFFERED → ACCEPTED 或 DECLINED。企业可将 SUBMITTED、VIEWED、INTERVIEW 转为 REJECTED；学生可将这三个状态转为 WITHDRAWN。企业可从 SUBMITTED 直接进入 INTERVIEW；其他跳转不允许。只有学生能接受/拒绝 OFFERED。终态为 ACCEPTED、DECLINED、REJECTED、WITHDRAWN，不可普通接口回退。初版同一岗位终态后不重新投递，页面明确说明。

HR 列表查询不自动将所有记录标记 VIEWED；单条查看动作可显式触发一次事件。状态变化返回新的 version；重复提交同一目标状态可幂等返回当前结果，不重复写事件。

## 6 匹配和推荐算法

### 6.1 两个分数的含义

`matchScore` 表示学生与单个岗位的条件匹配程度，范围 0 至 100，资料不足时为 null。`recommendationScore` 是候选中的推荐排序分，融合匹配、历史兴趣和热度，同样以 0 至 100 展示但不得称为“录用概率”。两个分数独立返回，企业排序默认按 matchScore。

### 6.2 先执行硬过滤

所有列表、详情、推荐和投递都校验岗位审核通过、招聘中、未到期、企业未被停用。个性化推荐进一步排除已投递和 HIDE 岗位，只保留 accept_fresh_graduate=true 的岗位。

岗位要求学历或毕业年份时，学生已知资料必须满足；若学生缺该字段，该受限岗位不进入个性化推荐，返回缺失字段提示，可在公共岗位浏览查看要求。学历与毕业年份都不设置限制的岗位可以继续参与。城市与薪资默认软偏好，用户明确启用筛选时才成为硬条件。禁止使用性别、姓名、手机号、民族或学校排名作为得分特征。

### 6.3 内容匹配 C

采用技能、岗位类别、城市、最低月薪四个维度，默认权重分别为 0.50、0.20、0.15、0.15。权重是初始设计参数，不宣称最优，实验后如调整必须升算法版本并记录验证集依据。

技能向量只含 SKILL 标签。学生熟练度 p_i 与岗位要求 q_i 都取 1 至 3，岗位重要度 w_i 取 1 至 3，统一标签别名后再算。余弦使用学生向量 p_i 与岗位向量 q_i，非共同技能补 0。

```text
cosine = dot(p,q) / (norm(p) * norm(q))
coverage = sum_over_job_skills(w_i * min(p_i/q_i, 1)) / sum_over_job_skills(w_i)
skill = 0.5 * cosine + 0.5 * coverage
category = 期望类别与岗位类别相同 ? 1 : 0
city = 岗位城市在期望城市集合中 ? 1 : 0
salary = 岗位最高月薪 >= 学生最低期望月薪 ? 1 : 岗位最高月薪/学生最低期望月薪
C = sum(available_weight * dimension_score) / sum(available_weight)
```

所有子分限制在 [0,1]，计算用未舍入值，输出保留四位小数；matchScore=100*C，显示一位小数。零范数返回 0，不产生 NaN。岗位没有技能要求或学生尚未填写技能时，技能维度 unavailable，而不是满分；填写了技能但没有交集则为 0。类别/城市未填写、薪资面议或期望最低月薪为 0 时，相应维度 unavailable。不可用维度不进分母；四个维度全不可用则 C=null。

额外学生技能可能改变余弦，这是模型定义，并不代表新增技能有害。coverage 与“缺失技能清单”用于解释岗位要求满足程度；实验保留纯余弦基线以观察差异。已匹配技能为交集中熟练度达到要求的项；不足技能包含缺失项和低于要求项，说明当前与要求等级。

例：四维子分为 0.8、1、1、0.5 时，C=0.825，匹配分 82.5。该数字仅为手算验收样例，不是运行或实验结果。

### 6.4 协同过滤 CF

使用用户与岗位的隐式行为矩阵。每个用户每个岗位在计算窗口内的最高有效兴趣权重为：浏览=1、收藏=3、投递=5，取 max 不无限累加浏览；窗口最近 90 天。收藏取消不保留活跃收藏权重，历史投递即使撤回仍可表示当时兴趣；HIDE 只用于个人候选排除。按时间顺序重放事件得到 cutoff 时状态，不读取未来状态。

对用户兴趣向量计算余弦相似度。目标用户至少有 3 个不同岗位的有效正反馈才尝试 CF。邻居按相似度降序取前 20 个，仅保留相似度大于 0 且至少有 2 个共同交互岗位的用户；并列按用户 id 升序。

```text
rawCF(u,j) = sum_neighbor(sim(u,v) * r(v,j)/5) / sum_neighbor(sim(u,v))
CF(u,j) = rawCF(u,j)
```

没有该岗位反馈的邻居贡献为 0，CF 已在 [0,1]，不在每页重新归一化。无有效邻居时 CF unavailable；有邻居但某岗位无人反馈时该岗位 CF=0。

### 6.5 热度 H 与混合排序

从 cutoff 前 30 天、去重用户行为统计收藏人数 f 和投递人数 a；同一人最多计一次，收藏取消不计活跃收藏。h=3*f+5*a，pop=log(1+h)/log(1+maxCandidateH)，全零时 pop=0。fresh=exp(-发布天数/30)，发布天数最小 0。H=0.8*pop+0.2*fresh。不得使用当前请求之后产生的浏览、收藏或投递。

常规 R=0.60*C+0.25*CF+0.15*H。用户无有效邻居时去掉 CF 并把剩余权重按比例归一：R=0.80*C+0.20*H。单岗位 C 不可用时去掉 C，再对可用项重归一；C 和 CF 都不可用则 R=H，matchScore=null，标记 HOT_FALLBACK。H 始终可用。

实际可用权重必须返回，供解释和重算；不能在冷启动时以 CF=0 仍保留 25% 权重。候选不足 K 就返回实际数量，不补入已下架或不满足学历的岗位。排序为 R 降序、C 降序且 null 最后、发布时间降序、id 升序。

### 6.6 候选、分页和缓存

毕业设计验收规模为 1,000 名模拟学生、2,000 个岗位、20,000 条行为。对全部合格候选批量加载标签和画像再评分，不使用“最新 200 个”截断。禁止逐岗位调用数据库读写分数形成 N+1；基线 computeAndSave 可用于独立详情，批量推荐使用纯函数批处理。

创建推荐批次时固定候选与评分，保存前 200 条排序结果（对全部候选评分后截取），批次分页每页默认 10、最大 50，10 分钟有效。limit=200 是展示上限，不是候选召回上限。返回 totalEligible 和 totalRanked；查询批次只能由所属用户访问。

画像、简历、岗位目录、标签权重、行为或算法版本变化，使相关批次过期；单实例可使用持久化全局目录/标签/行为版本换取简单正确性，版本变化不得每次计数因重复事件增加。分页前重新校验动态资格，若快照中岗位下架、过期或用户已投递则返回 409 BATCH_STALE，要求刷新；禁止悄悄继续展示不可投递岗位。

初版推荐可不缓存计算结果，优先正确；若保留 match_score，其 key 必须包含 resume/profile/job/tag/algorithm 版本，并发使用 upsert 或唯一冲突重读。事务回滚不能留下指向未提交数据的可用缓存。推荐批次保留最近 7 天用于排错，定时清理只删本系统过期记录。

### 6.7 推荐结果契约

```json
{
  "jobId": 101,
  "matchScore": 82.5,
  "recommendationScore": 74.0,
  "algorithmVersion": "hybrid-v1",
  "strategy": "CONTENT_HOT",
  "components": {"content": 0.825, "collaborative": null, "hot": 0.4},
  "effectiveWeights": {"content": 0.8, "collaborative": 0, "hot": 0.2},
  "dimensions": {"skill": 0.8, "category": 1, "city": 1, "salary": 0.5},
  "matchedSkills": ["Java"],
  "missingSkills": [{"name": "Redis", "actual": 0, "required": 2}],
  "missingProfileFields": [],
  "reasons": ["岗位类别符合求职意向", "工作城市符合偏好"],
  "fallbackReason": "NO_VALID_NEIGHBORS"
}
```

这是契约样例，recommendationScore=100*(0.8*0.825+0.2*0.4)=74.0。reason 由实际特征生成，不硬编码所有卡片相同的“高度匹配”。不对用户暴露其他学生 id、简历或具体行为。

## 7 接口设计

以下为目标 API，M0 后生成 OpenAPI 或等价机器可读契约。底座相近接口可保留，通过适配映射到同一 Service；旧接口不得绕过新权限和状态规则。

JSON 成功包装 `{code:"OK",data:...,requestId:"..."}`；错误包含 `{code,message,requestId,details}`，不泄露 SQL 和堆栈。401 未登录、403 无角色权限、404 不存在或对象不可见、409 重复/版本冲突、422 字段错误、429 限流。不能所有错误都返回 HTTP 200。分页 `{items,total,page,pageSize}`，普通列表稳定按 id 排序。

| 方法和路径 | 权限与输入 | 输出或约束 |
| --- | --- | --- |
| POST /api/auth/register | 匿名；username/password/email/role | 只允许 USER、HR；demo 模式标明邮箱未验证 |
| POST /api/auth/login | 匿名；账号密码 | Sa-Token 令牌及角色；登录限流 |
| POST /api/auth/logout | 已登录 | 注销当前会话 |
| GET /api/auth/me | 已登录 | 无密码的当前用户 |
| GET / PUT /api/student/profile | USER；画像 DTO | 更新 profile_version；禁止写 role/status |
| GET / POST /api/student/resumes | USER | 本人列表或创建 |
| GET / PUT / DELETE /api/student/resumes/{id} | 本人 | DELETE 为归档；已有投递快照保留 |
| PUT /api/student/resumes/{id}/default | 本人 | 原子切换默认简历 |
| POST /api/student/resumes/{id}/attachment | 本人；PDF multipart | 文件 id；最大 5 MB，允许结构化简历不上传附件 |
| GET /api/student/resumes/{id}/attachment | 本人 | 下载当前简历附件，不能通过 URL 绕过权限 |
| GET /api/jobs | 公开；keyword/city/category/salary/page | 公开且有效岗位 |
| GET /api/jobs/{id} | 公开 | 岗位完整条件；禁止学生隐私混入 |
| GET /api/student/matches/{jobId}?resumeId= | USER，本人简历 | 匹配详情或硬条件不符合原因 |
| POST /api/student/recommendation-batches | USER；resumeId 可选 | batchId、版本、首屏、候选数、资料提示 |
| GET /api/student/recommendation-batches/{id}?page= | USER，本人批次 | 固定排序分页；过期或版本变化返回 409 |
| PUT / DELETE /api/student/favorites/{jobId} | USER | 收藏/取消，幂等 |
| PUT / DELETE /api/student/hidden-jobs/{jobId} | USER | 不感兴趣/恢复，幂等 |
| POST /api/student/applications | USER；jobId/resumeId | 服务端填 user_id；快照与唯一性 |
| GET /api/student/applications | USER | 本人投递状态和时间线 |
| PATCH /api/student/applications/{id}/status | 本人；status/version | 仅学生允许的转移 |
| GET / POST /api/hr/jobs | HR 已通过审核 | 本账号岗位列表或创建 |
| PUT /api/hr/jobs/{id} | 本人岗位；DTO/version | 更新后重新审核 |
| PATCH /api/hr/jobs/{id}/status | 本人岗位；状态/version | 关闭、开放、归档 |
| GET /api/hr/jobs/{id}/applications | 本人岗位；排序/分页 | 已投递简历摘要和匹配分 |
| GET /api/hr/applications/{id}/resume | 对应岗位 HR | 投递时结构化快照 |
| GET /api/hr/applications/{id}/attachment | 对应岗位 HR | 经鉴权下载投递时文件版本 |
| PATCH /api/hr/applications/{id}/status | 对应岗位 HR；status/version | 合法流转和事件 |
| GET / PATCH /api/admin/hr-audits/{id} | ADMIN | 企业审核与原因 |
| GET / PATCH /api/admin/job-audits/{id} | ADMIN | 岗位审核与原因 |
| GET / PATCH /api/admin/users/{id} | ADMIN | 查看必要信息、启停；不允许停用最后一名管理员 |
| GET / POST / PUT / DELETE /api/admin/tags | ADMIN，更新/删除带 id | 字典变更使对应算法输入版本失效 |
| GET /api/admin/statistics | ADMIN；from/to | 明确分母、时间范围和统计口径 |
| CRUD /api/admin/notices | ADMIN | 公告增改/归档；另有公开只读接口 |

身份从服务端会话取得，不接受客户端 userId/hrId 作为授权依据。GET 不产生投递或修改状态；浏览行为通过去重事件入口或显式详情曝光事件记录，避免预取重复累计。公共标签与类别字典接口由实施阶段补入 OpenAPI。

## 8 页面与交互

学生：岗位首页与筛选、推荐列表、匹配详情、画像编辑、简历管理、收藏、投递进度。推荐卡显示“匹配分”和具体原因；热度兜底卡显示“完善资料可获得个性化匹配”，matchScore=null 不显示 0% 或 100%。

企业：审核进度、岗位列表与编辑、投递管理、候选匹配详情。职位编辑显示审核状态；查看学生简历只通过投递记录入口。管理员：审核队列、用户启停、标签与公告、基础统计、实验结果。

所有列表有加载、空结果、权限不足、请求失败和重试状态。表单有必要字段校验；破坏性操作二次确认；主要页面适配 1440px 桌面与 390px 手机宽度，表格可横向滚动。前端不伪造后台不存在的数据，不把本地数组替代业务 API。

## 9 安全与数据保护

- 后端所有敏感查询附带对象归属条件，不能只靠隐藏按钮。管理员启停账号时同步撤销会话或在请求时检查状态。
- 文件只允许 PDF，校验扩展名、MIME 和文件头，5 MB 上限；随机文件名、私有目录，拒绝路径穿越。附件不由公开静态目录直接提供，下载要求归属鉴权。
- 投递快照引用独立文件版本；学生换附件不能覆盖旧文件，仍有投递引用时不能物理删除。
- SQL 使用参数绑定；排序字段白名单。公告和岗位正文默认纯文本，若使用富文本必须清洗。
- 日志不得包含密码、token、完整手机号、附件正文。密钥和连接密码只在环境变量或不入库的本地配置。
- 浏览器会话方案由 ADR 固定；沿用 token header 时不混用未保护的 cookie 登录。生产需 HTTPS；CORS 限定来源。
- demo 数据全部合成，界面与报告注明；不采集真实学生简历，不把上游演示个人信息当可公开数据。

## 10 评估设计

实现 `evaluation/run` 入口，输出 JSON/CSV、配置、数据哈希、种子、代码 SHA 与报告。算法计算逻辑应直接调用后端相同核心模块或共享固定输入输出样例验证一致性，不另写一套不一致公式。

数据分三层：单元黄金样例验证数学和边界；固定 seed=20261027 的合成业务数据验证流程与负载；若后续获得授权的真实匿名行为数据，再开展外部有效性实验。合成数据指标只能证明该合成设置下结果，不证明真实就业效果，也不能把根据同一评分公式生成的标签当独立真实偏好。

有行为时间戳的数据采用全局时间 cutoff，前 70% 时间范围训练、中间 15% 验证、后 15% 测试，实际时间边界写入 manifest。调参只看验证集。每个样本按预测时点重建已发布且合格岗位、画像、标签、行为和取消状态；测试正反馈不参与用户向量、热度、候选过滤和缓存生成。先推荐、后发生的投递才可作为相关标签。

对同一测试用户与候选集合比较 HOT、CONTENT、HYBRID；额外消融去掉 CF、去掉冷启动回退。训练阶段完全无行为用户单列冷启动组。测试集没有合格正样本的用户不计入 Recall/NDCG 平均，并单独报告人数；Precision 分母固定 K，返回不足 K 的位置按不命中处理。

指标：Precision@5/10、Recall@5/10、NDCG@5/10、HitRate@5/10、Coverage@10、FallbackRatio。Recall 分母是该测试用户在预测时点可推荐候选中的相关岗位数；NDCG 使用二元相关性、log2(rank+1) 折损；Coverage 分母是该实验统一评估目录内至少对一位用户合格的岗位去重数；FallbackRatio 为进入 HOT_FALLBACK 的用户批次数/总批次数。同步报告平均候选数量、无候选人数、请求时延与总样本数。

优先用全候选排序；若后续采用负采样，单独标注采样分布和种子，不能与全候选结果直接比较。没有真实数据或 HYBRID 未超过 CONTENT 均可如实写成结果，不以改标签、删用户或反复看测试集调权重制造提升。

## 11 测试与性能验收

必须覆盖：完全匹配、不重叠技能、零向量、技能缺失、无工资、无偏好、学历不足、毕业年份未知、全部候选失效、无历史用户、无邻居、排序并列、已投递排除、HIDE 恢复、画像和标签更新后旧分失效。

数据库和接口测试：重复投递并发只能成功一次；改状态并发只能有一条合法成功结果；关闭岗位与投递竞争不产生关闭后的新投递；跨学生和跨企业 IDOR 被拒绝；禁用账号旧令牌失效；更换简历后历史快照不变；岗位归档不丢投递。

端到端：HR 注册待审 → 管理员审核 → 发布岗位待审 → 岗位通过 → 学生填画像 → 推荐和匹配详情 → 投递 → HR 筛选并面试/录用 → 学生接受 → 管理员统计可追溯。

性能目标是待测验收阈值：在记录了 CPU、内存、JDK、数据库与配置的本机环境，用 1,000 学生/2,000 岗位/20,000 行为，10 并发、预热 30 秒、采样 60 秒，普通列表 P95 < 500ms、首次推荐批次 P95 < 2s、错误率 < 1%。分别记录冷计算和批次翻页结果；超标需提供瓶颈与修复，不预填“已达标”。

## 12 开发与交付里程碑

M0：锁定底座、许可证与版本；原始启动、测试、页面和 API 冒烟；输出复用矩阵和差异方案，交审核。

M1：鉴权、数据迁移、学生画像、岗位审核与搜索；新旧接口不能形成旁路。

M2：内容匹配、匹配解释、冷启动与版本失效；黄金样例通过，形成可演示学生链路。

M3：收藏/隐藏、投递快照、企业处理与完整状态、私有附件、统计；完成三角色端到端流程。

M4：CF 混合推荐、离线评估、权限与并发测试、性能记录、部署说明与演示证据；所有 P0/P1 完成后才能最终验收。

每阶段提供当前 SHA、相对上个审核版本的 diff、执行过的命令和退出码、真实截图或录像、未完成项。每阶段先自测再申请审核；阶段审核是本交接包定义的合作流程，不是未经验证的自动通过。
