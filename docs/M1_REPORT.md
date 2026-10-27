# M1 数据与权限交接报告

日期：2026-10-27。**状态：待审核**。M1 从审核通过的 M0 `2494b0b51f7f65264838a0729ca2876f561ff515` 分出 `m1/foundation`。最终 commit SHA、远程状态、工作区状态由提交后的交接消息提供；审核时以 `git rev-parse HEAD` 和远程分支实际值为准。M0 分支保留。项目 `/Users/guanghao/private/graduate-job-match`，私有仓库 https://github.com/jackyalana/graduate-job-match。

## 对本轮1–8项的实现与证据

| 项 | 代码位置 | 实际完成与验证 |
| --- | --- | --- |
| 1 附件 | config/WebMvcConfig、service/impl/FileStorageServiceImpl、ResumeServiceImpl、HrJobServiceImpl、UserResumeController、HrJobController | 移除根和简历静态映射；PDF扩展/MIME/头/5MiB、本人及对应投递HR鉴权。真实HTTP测匿名直链404、匿名API401、伪PDF/MIME/扩展拒绝、超限413、本人/对应HR下载成功，跨学生/跨HR拒绝。已有投递附件暂不允许删除/替换；历史快照留M3 |
| 2 认证/响应 | config/SaTokenConfig、SaTokenStpInterfaceImpl、RequestIdFilter、WebMvcConfig、common/Response、exception/GlobalExceptionHandler、frontend/src/api/request.js | header token、每请求账号状态、停用注销旧token；CORS白名单、不读cookie；真实401/403/404/409/413/422/429及requestId，前端错误同步适配。普通不可投递/无效简历目前为HTTP400，见契约 |
| 3 画像/数据 | backend/sql/migrations/V001__m1_foundation.sql、StudentProfile*、Job/JobSaveDTO、frontend/src/pages/StudentProfile.vue | 画像、城市偏好、类别、岗位结构化条件/版本及邮箱验证字段；空库和原上游schema升级实测，旧学历映射见 M1_MIGRATION.md |
| 4 审核/资格 | service/impl/JobEligibilityServiceImpl、JobBrowseServiceImpl、HrJobServiceImpl、AdminAuditServiceImpl；frontend Home/JobManage/AuditManage | 公开岗位检查审核/在招/期限/HR状态；个人推荐附加应届、学历、毕业年及已投递排除；未知画像提醒；岗位条件和标签修改回待审。审核决策验证版本，旧版本409。新旧浏览接口复用同一规则，结构化类别/城市/薪资筛选真实验证 |
| 5 简历/历史保护 | ResumeServiceImpl、JobBrowseServiceImpl、ApplyServiceImpl、HrJobServiceImpl、AdminUserServiceImpl | 明确校验本人有效简历；无有效默认不暗选其他简历。旧简历删除改归档、旧岗位remove改关闭、旧用户物理删除暂拒绝409；真实检查投递行仍在。完整不可变快照和状态机留M3 |
| 6 文档/脚本 | docs/UPSTREAM_API_MAPPING.md、M1_API_CONTRACT.md、adr/0002-m1-session-and-data.md、scripts/m0/local.py、scripts/m1/stop.py | common修正为 USER/HR/ADMIN 的有效会话；明确SDD优先；停止前检查PID、启动时间、命令及进程组，身份不匹配拒绝。旧M0无身份元数据的pid文件会拒绝停止，需人工核实 |
| 7 新测试 | scripts/m1/http_tests.py、setup_test_databases.py、normal_registration_test.py、test_stop_identity.py、restore_test.py | 128次真实HTTP请求达到预期；空库/上游schema/回填/重复迁移、普通注册隔离、进程误杀防护、备份到新库并核对6表实际执行。依赖真实MySQL，不仅是原11项单元测试 |
| 8 交付 | 本报告、M1_MIGRATION.md、M1_API_CONTRACT.md、openapi-m1.json、docs/evidence/M1/ | 不进入M2/M3或框架大版本升级。M1 与 M0 可用 `git diff 2494b0b51f7f65264838a0729ca2876f561ff515 HEAD` 核对 |

## 实际构建、测试及启动

命令、cwd、版本、退出码见 `evidence/M1/commands-and-exit-codes.md`。最终 `backend-package-handoff.log`：11 原有JUnit测试、0失败/错误/跳过并打包成功；另有 `http-tests-handoff.log` 中128次真实HTTP请求及数据库断言。`normal-registration-handoff.log` 的普通dev配置即使误设demo开关，仍拒绝无验证码及固定 000000 验证码注册。`frontend-build-verified.log` 成功，存在大chunk警告。`migration-handoff.log` 对空库及合成上游数据升级成功；`restore-test.log` 在新隔离库恢复并核对6表行数。

已执行 demo/browser：登录、画像保存/刷新、HR新建岗位、管理员审核、演示注册及资料提示；截图范围和中间版本注明在 browser-observations.md。邮件验证码通过真实SMTP的全流程、Redis、移动端和性能未执行。

当前M1前端 http://127.0.0.1:15174/login ，后端 http://127.0.0.1:18081 ，独立MySQL 127.0.0.1:13306（运行库名见 `.local/m1-schemas.json`）。测试数据全部合成。账号 `demo_student` / `demo_hr` / `demo_admin`，本机查看密码：`python3 scripts/m0/local.py accounts`；不要将输出发送或入库。demo注册仅本机，邮箱未验证。进程实时状态可能改变。

已准备好本地运行时和 M0 私有MySQL时，项目根目录可运行：

```bash
# 每次创建新的隔离合成库；不要在M0或真实库上重放
python3 scripts/m1/setup_test_databases.py
export JAVA_HOME="$PWD/.local/jdk"
.local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package
python3 scripts/m1/start.py
python3 scripts/m1/http_tests.py
python3 scripts/m1/frontend.py
# 停止前后端（先核对身份，不停止MySQL）
python3 scripts/m1/stop.py
```

`setup_test_databases.py` 假定 M0 私有工具、随机凭证和MySQL 13306已存在；无缓存的全新机器需先按 BASELINE 获取依赖。它每次新建测试库，不会清理旧库；重跑HTTP测试前必须新建新库，不要在已有演示库直接重复写入。`http_tests.py` 会故意归档一个合成默认简历、关闭岗位并切换账号状态，审核时如需测试应使用独立新库。

## 数据、接口和依赖变更

V001 对原schema增量增加 `student_profile`、`student_city_preference`、`job_category`、结构化岗位字段、`resume.version`、`job.version`、`sys_user.email_verified`；具体SQL、旧学历规则和恢复流程见 M1_MIGRATION.md。MySQL DDL 非原子，失败须从备份恢复到新隔离库；没有修改M0或真实库。

JSON code 从数字0改为 `OK`，错误改为真实状态+字符串code+requestId；新增画像、类别、SDD岗位浏览及附件别名。岗位审核决策现在必须带列表返回的version，旧审核调用不带version返回409。common接口仍由三角色有效会话使用；前端已适配。`M1_API_CONTRACT.md` / `openapi-m1.json` 记录当前契约及未完成接口。前后端技术版本、POM与lockfile未升级；普通数据库密码改为环境变量，没有把demo密码提交。

## 尚未完成或需审核的边界

- M2评分、解释、完整冷启动、全部候选批处理及版本化推荐批次未实施。原推荐仍是M0余弦、最新200候选和无简历随机排序；页面明确标识旧标签相似度，不应当作SDD匹配分或录用概率。M0并发算分唯一冲突尚未修复。
- M3投递快照、文件历史版本、唯一用户岗位约束、状态机、行为/隐藏、投递与关闭竞争的事务仍未实现；已有投递附件临时禁止替换，不能替代历史快照。旧岗位remove是关闭，不是独立 ARCHIVED 状态。
- 学历/年份未知时个性化推荐排除受限岗位并提示；公共浏览和旧投递只检查公开有效性与本人有效简历。若希望将个人学历/年份限制强加于投递，需要审核明确并提交ADR。
- 账号禁用与两组越权场景已真实测试，但并未穷举所有IDOR、并发状态竞争和攻击变体。单实例内存限流不代表分布式/反向代理防护；localStorage方案需要后续XSS和部署安全审计。
- `GET /api/jobs` 和资料/附件接口有新契约；部分SDD目标资源路径、真实HTTP错误码精细映射、完整OpenAPI和全部页面空态待后续完善。邮箱真实验证、真实SMTP、真实学生/招聘数据均未测试。
- 上游旧DATETIME来源时区未知，必须在实际数据迁移前确认；本合成库与服务按UTC，当前页面按上海时间显示。

## 审核退回后的修复

基于 `690d7d0d7d5f527f0b3b17a7ce0de45a5b9993d3` 修复：已有账号按数据库识别的用户 ID 限制用户名及邮箱登录，大小写、重音变体共享计数；未知账号按小写输入计数。该限流仍仅适用于单实例。

管理员详情展示结构化条件、技能标签及版本，决策使用所查看版本；旧版本返回 409。查询、路径类型错误及必填参数缺失返回 422。岗位标签保存后刷新列表，立即编辑可使用最新版本。

修复后重新构建，11 项 JUnit、31 次新增真实 HTTP 请求、131 次原有 HTTP 回归和前端 build 均成功。浏览器实测保存标签后立即编辑，以及查看审核详情。命令、结果和截图见 `evidence/M1-review/README.md`。此前 `evidence/M1/` 包含首次提交时的原始证据及历史 manifest，不代表修复后的文件哈希。

**待审核。**
