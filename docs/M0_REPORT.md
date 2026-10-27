# M0：原底座复现、审计与交接

日期：2026-10-27（Asia/Shanghai）。**状态：待审核；M0 执行结束，尚未获得准入结论。未进入 M1–M4。**

## 1. 版本和范围

- 项目绝对路径：`/Users/guanghao/private/graduate-job-match`。
- 私有仓库：https://github.com/jackyalana/graduate-job-match ，分支 `m0/baseline`。完整交付 commit SHA 随交接消息提供；本文件不把自身父提交写成最终 HEAD。审核时读取 `git rev-parse HEAD`、`git status --short --branch`，并对齐远程该分支。
- 上游固定 SHA：`cd755d13443ea4b4c98be86e6c692238ed566d31`，保留 Git 历史、LICENSE 与版权。
- 相对上次审核：首次提交。新增 demo profile、私有运行与合成数据/验证脚本、准备包原件、五份报告/来源说明/ADR、脱敏日志与截图。`.gitignore` 额外忽略 `.local/` 和 Python 缓存；README 增加 M0 入口。
- **原业务 Java、原测试、前端源码、POM、package.json 和 lockfile 无改动**，见 `evidence/M0/artifact-manifest.json`。没有提前实现新评分、状态机或数据模型；没有升级全部依赖。

## 2. 本轮需求与实际结果

| M0 要求 | 实际完成位置/结果 |
| --- | --- |
| M0-1 独立目录、来源锁定 | 原目录不存在；新克隆、先 detached 指定 SHA 再创建 m0/baseline；未触及其他工作树。BASELINE / LICENSE / THIRD_PARTY_NOTICES |
| M0-2 环境和依赖审阅 | POM、lockfile、配置、三类 dev 脚本、SQL 已阅读；environment.txt、下载哈希；缺项用项目私有工具补齐，Redis 选择 memory |
| M0-3 隔离数据/禁用外部服务 | local.py 确认新实例仅系统库后导入 schema；显式 demo profile；seed.py 生成6个随机密码合成账号，不导入上游人名/公司/头像种子，不调用 SMTP/模型 |
| M0-4 测试与构建 | 原后端 11 测试、package、lockfile npm ci、Vite build 均实际成功；早期失败保留，见下一节 |
| M0-5 真实运行 | API主记录59条（含20个并发请求的响应）；浏览器三角色登录、HR发布、管理员审核、学生浏览/保存简历/投递；JVM重启后数据仍在 |
| M0-6 复用审计 | 鉴权/注册/文件/审核/投递/算法/缓存调用链；已复现缺陷与静态风险分开列在第5节 |
| M0-7 文档 | BASELINE、REUSE_MATRIX、M0_REPORT、UPSTREAM_API_MAPPING、THIRD_PARTY_NOTICES 均已提供 |
| M0-8 版本决定 | ADR-0001：本地保持上游版本；Java17/Boot3及风险依赖更新列为独立提案，不宣称版本安全或生产就绪 |

FR01–FR12 是后续 SDD 目标，本轮只验证上游相近路径。不能因三角色冒烟成功就把 FR01/FR08/FR09 标记为全部完成。逐 FR 位置与边界见 REUSE_MATRIX；54条实际路由见 UPSTREAM_API_MAPPING 和 upstream-routes.json。

## 3. 实际执行的测试与构建

命令工作目录默认根路径下表指定的子目录。包装器 `scripts/m0/record.py` 保存 start/end UTC、argv、cwd、真实退出码与日志；日志时间与中文交付日期分别标注，不改写原输出。硬件/安装早期观察另见 bootstrap-observations.md。

| 命令 / cwd | 退出码 | 实际结论和证据 |
| --- | --- | --- |
| `mvn -B -Dmaven.repo.local=<项目>/.local/m2 test` / backend，JAVA_HOME=.local/jdk | 0 | backend-tests-retry.log：Tests run 11, Failures 0, Errors 0, Skipped 0，5个测试类；JUnit/Mockito/配置/缓存测试，不等于数据库权限/并发全面覆盖 |
| 同样环境 `mvn ... package` / backend | 0 | backend-package.log：再次执行11个测试，打包可运行JAR成功；没有 -DskipTests |
| `npm ci --ignore-scripts --no-audit --no-fund --fetch-retries=0` / frontend | 0 | frontend-install-final.log：严格 lockfile 安装78包，不改依赖；生命周期脚本禁用 |
| `VITE_API_BASE_URL=http://127.0.0.1:18080 npm run build` / frontend | 0 | frontend-build-retry.log：Vite成功，有依赖 PURE 注释和大 chunk 警告，不掩盖警告 |
| `python3 scripts/m0/smoke.py` / root | 0 | api-smoke.log + smoke-results.json；正常流程断言通过，已知原缺陷按观察捕捉，不表示所有响应都成功 |
| `python3 scripts/m0/probes.py` / root | 0 | supplemental-probes.log/json；复现200截断、失效简历、审核和禁用会话；错误情况是发现的问题而非验收通过 |
| `python3 scripts/m0/restart_check.py` / root | 0 | restart-check.log/results：真实停启JVM、新密码登录、投递仍有3条，库内users/resumes/jobs/applications数量不变 |
| 浏览器 CUA 实际交互 / 127.0.0.1:15173 | 非shell命令，无退出码 | screenshots/ 与 browser-observations.md；真实界面保存、审核和投递，未用前端mock |

早期失败及修复均保留：

- 首次 `git clone` 受限网络 DNS 失败(exit 128)，获准联网后克隆成功；没有因拉取失败切换底座。
- 首次 tar Python 3.9 不支持 `extractall(filter=...)`，未成功解包；改为检查官方归档路径再解包。JDK压缩包实际为 Contents/Home，最初软链接多写 zulu-8.jdk，导致第一次 Maven exit 1；修正私有链接后成功。两者是复现工具错误，不是上游业务缺陷。
- 首次受限 npm ci 卡住；联网重试曾与旧进程重叠，旧进程随后终止(exit 1)，一次 build 报 `vite: command not found`(127)。确认旧进程终止后串行 npm ci(final) 并 build(retry)成功；未改 lockfile。该中间失误没有作为成功证据。
- MySQL第一次受限初始化 SIGSEGV(exit 2)，同版本在另一个全新目录以获准本机进程环境重试成功(0)；没有删旧目录或升级替代数据库。不能仅据这一次比较确定根因就是沙箱，错误日志保留。

未执行/不适用：Redis实机、Windows脚本、Compose、第二台干净机器安装、正式邮箱验证码注册、全量文件攻击/IDOR回归、SDD全状态/快照/性能/评估。现有后端测试中 `MatchServiceImplTest` 两例只验证L1/L2缓存返回；数学边界仍需M2黄金样例。没有测试被标成“跳过后通过”。

## 4. 运行与演示

当前服务仅本机：前端 http://127.0.0.1:15173/login ，后端 http://127.0.0.1:18080 ，MySQL 13306。启动/停止、空库初始化、恢复详见 BASELINE。运行后通过 `python3 scripts/m0/local.py status` 核对，服务重启或机器睡眠后需重新检查，不能把本报告当实时状态。

本机账号：`demo_student`、`demo_hr`、`demo_admin`。随机密码用 `python3 scripts/m0/local.py accounts` 在本机查看；不得把该输出加入证据/GitHub。正式邮件未配置，使用用户名密码登录。重启后的旧浏览器令牌无效，需重新登录。

已验证演示：HR发布合成岗位→待审不公开→管理员批准→学生看到岗位→保存合成简历→标签真实算分→投递→HR本人列表查看。浏览器操作另建立 Vue 岗位id=3和简历id=3并真实投递；API主样例id=1。截图是证据之一，配套真实请求、SQL计数和重启验证。不能把“高匹配”标签当成新算法结果。

合成环境已有故意构造的重复投递、伪附件和下架探针，均标记 M0。不得拿当前库当论文实验集。接口样例已脱敏，见 UPSTREAM_API_MAPPING；数据库不上传dump，仅记录schema/计数/错误计数及合成API样例。

## 5. 已确认缺陷与待验证风险

源码位置相对 `backend/src/main/java/com/lanrecruitment/`。以下优先级是工程审计建议，最终由审核者判定；旧行为在M0未修复。

| ID / 建议级别 | 证据、触发与当前结果 | SDD期望 / 后续 |
| --- | --- | --- |
| B01 / P0 | config/WebMvcConfig.java:48、FileStorageServiceImpl.java:26；上传后用无token GET `/uploads/resume/<id>.pdf` 实际HTTP200，返回合成文件正文 | 私有目录、下载必须校验归属，M1/M3阻断真实资料进入 |
| B02 / P1 | FileStorageServiceImpl.java:78，store只查扩展名/10MB；`synthetic-not-pdf.pdf` 非PDF正文被上传成功 | 5MB、PDF扩展/MIME/文件头、历史引用；M3 |
| B03 / P1 | ApplyServiceImpl.java:65–78 + init.sql `uk_resume_job`；同一学生两个resume投同job都成功，SQL显示copies=2 | UNIQUE(user_id,job_id)，事务岗位锁，冲突409；M3 |
| B04 / P1 | HrJobServiceImpl.java:369–383；拒绝3后再次提交0成功，未校验前态/version/事件 | SDD完整状态机和双方权限，M3 |
| B05 / P1 | HrJobServiceImpl.java:192–202；独立删除探针job投递数从1变0；ResumeServiceImpl.java:143和AdminUserServiceImpl亦有物理删关联路径（后两处静态确认） | 归档与不可变快照，禁止旧接口旁路；M3 |
| B06 / P1 | JobBrowseServiceImpl.java:136–142；203个合格岗位时201个新零向量岗位挤掉旧满分job1，首50均0分 | 全合格候选批量算分后截展示上限，M2 |
| B07 / P1 | JobBrowseServiceImpl.java:117–127 `rand(userId)`，无简历返回null分；未含热门/新鲜度解释 | SDD确定性热度兜底和资料提示；不是个性化，M2 |
| B08 / P1 | MatchServiceImpl.java:47–66：先查后插、先写缓存再插DB；20并发匹配17成功/3业务500，MySQL ER_DUP_ENTRY=3，DB仍单记录 | 原子upsert/冲突重读、版本key、提交后缓存；M2。通用错误不暴露具体异常，1062计数与调用链相互印证，不声称有完整SQL堆栈 |
| B09 / P1 | JobBrowseServiceImpl.java:160–181不检查简历status；将本人简历置0后recommend仍成功给分 | 仅本人有效默认/显式简历；M1/M2 |
| B10 / P1 | HrJobServiceImpl.java:239 saveJobTags未设PENDING；审核通过job修改技能后audit_status仍1 | 所有影响条件/技能编辑回待审、统一旧接口；M1 |
| B11 / P1 | SaTokenConfig对common仅checkLogin；UserServiceImpl.currentUser不查status；禁用学生后旧token common/me仍code0，角色resume返回403 | 撤销或每请求统一查禁用，不仅角色路由；M1 |
| B12 / P1 | GlobalExceptionHandler + SaTokenConfig；未登录/跨角色/业务404实际HTTP200，code体内区分 | SDD真实401/403/404/409/422与requestId，M1 |

静态已确认的缺口：无毕业生画像、硬学历/毕业年/截止过滤、CF/热门评估、行为/收藏/隐藏、投递快照/事件/版本；推荐不排除已投递；公共查询和投递未校验HR禁用。TagServiceImpl.save/delete不更新已存关联权重或清理match缓存；cache key仅resume/job，DB旧分无版本；saveJobTags事务提交前清内存、Resume.saveTags缺整体事务，存在失效竞态。宽CORS/withCredentials/cookie需M1会话ADR收敛。

待补证而非运行结论：并发投递/关闭岗位竞态、默认简历原子切换、缓存提交回滚竞态、字典权重修改端到端、全部跨对象IDOR、路径穿越/大文件/文件头全集、XSS、审计/统计口径、压力P95、真实数据效果。本次跨学生detail和跨HR applications各有一个拒绝样例，不能称权限全面通过。

## 6. 迁移、依赖与需求偏离

没有业务数据迁移；只以原DDL创建独立新库并改执行时库名，随机账号由seed.py生成。配置仅demo profile、UTC、loopback端口/私有上传位置；普通profile无通用验证码，原HTTP路径不变。未修改任何POM/lockfile依赖版本。框架升级提案见ADR-0001。

M0允许的复现修复只有工具路径/安装顺序/运行权限和显式配置。没有偷偷调整SDD算法或数据模型。当前原底座距SDD存在上表缺口；请求审核确认复用路线和后续修复顺序，不能将这些缺口解释为已满足需求或擅自豁免。邮件注册改造、Compose、学校模板与截止时间尚未冻结。

## 7. 提交给审核者

请基于仓库实际SHA、diff、日志、API和浏览器复核。先阅读BASELINE、REUSE_MATRIX、UPSTREAM_API_MAPPING、ADR，再核查本报告中的每条缺陷。是否允许进入M1由审核决定；本次不替审核者写“最终通过”。

**待审核。**
