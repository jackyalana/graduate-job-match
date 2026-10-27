# 上游 API 与 SDD 映射

状态：M0 待审核。以下是实际 controller 路由，不是目标 API 已实现声明。源码根为 `backend/src/main/java/com/lanrecruitment/`。M0 没有改变接口，数字 `code=0` 为成功；多数失败也是 HTTP 200。目标 SDD 使用 OK/requestId 和真实 HTTP 状态，需要统一适配，不可让旧路径绕过。

## 需求映射

| SDD / FR | 目标接口族 | 当前上游接口族 | 改造与里程碑 |
| --- | --- | --- | --- |
| FR01 | /api/auth/register,login,logout,me | /api/public/auth/register,login/password,login/email,code；/api/common/auth/logout；/api/common/user/me | M1，保留角色和 BCrypt，限流/注销/禁用、HTTP 契约；邮件未启用，demo 预置 |
| FR02 | /api/student/profile；/api/student/resumes/{id},default,attachment | /api/common/user/profile,profile/update；/api/user/resume/list,detail,save,set-default,tags/save,file/* | M1/M3；新增 profile，旧 resume.education 迁移；私有附件和归档 |
| FR03 | /api/hr/jobs/{id},status | /api/hr/job/list,save,delete,remove,tags/save,tags | M1/M3；单一资格/审核/归档规则，不保留物理删除旁路 |
| FR04 | /api/jobs/{id} | /api/public/job/list,detail | M1；增量适配字段与全部有效条件 |
| FR05 | /api/student/matches/{jobId} | 无独立详情匹配 API；recommend 和 HR applications 内部调用 MatchService | M2；统一后端纯函数与解释；不伪造已有新接口 |
| FR06 | /api/student/recommendation-batches/{id} | /api/user/job/recommend?resumeId=&offset=&pageSize= | M2；全候选计算、批次/版本/409；停止随机与最新200召回 |
| FR07 | /api/student/favorites,hidden-jobs | 无 | M3 新增，事件与幂等 |
| FR08 | /api/student/applications/{id}/status | /api/user/job/apply；/api/user/apply/list,list/detail,resume/update | M3；快照唯一约束、学生状态；旧换简历接口须服从快照约束 |
| FR09 | /api/hr/jobs/{id}/applications；/api/hr/applications/{id}/resume,attachment,status | /api/hr/job/applications,apply/status；/api/hr/resume/file/download | M3；仅本 HR 已投递、状态机/版本/私有历史附件 |
| FR10 | /api/admin/hr-audits,job-audits,users,tags | /api/admin/audit/hr/*,job/*；/api/admin/user/*；/api/admin/tag/* | M1/M3；理由/审计、禁用会话、归属、字典版本失效 |
| FR11 | /api/admin/notices,statistics | /api/admin/notice/list,save,delete；/api/public/notice/list | 公告复用待专项验；统计新增 M3/M4 |
| FR12 | evaluation/run、实验展示 | 无 | M4 新增，禁止将当前探针当算法效果实验 |

## 本次实际请求样例

完整脱敏请求/响应在 `evidence/M0/smoke-results.json`（59 条主记录，内含20条并发响应），补充在 `supplemental-probes.json`。令牌和随机密码不入库。

```http
POST /api/public/auth/login/password
Content-Type: application/json
{"username":"demo_student","password":"<从本机 accounts 命令取得，不写入仓库>"}

HTTP/1.1 200
{"code":0,"message":"OK","data":"<TOKEN REDACTED>"}

GET /api/user/job/recommend?resumeId=1
satoken: <仅放当前会话变量>

HTTP/1.1 200
{"code":0,"message":"OK","data":[{"id":1,"matchScore":1.0000,"jobName":"M0 合成 Java 应届岗位"}]}
```

最后一个响应为主证据中单岗位时摘录，省略卡片其他字段，不是当前数据库完整结果。上游分数为 0–1 标签余弦，非 SDD 的 0–100 多维分，更不是录用概率。

浏览器新增岗位 id=3、简历 id=3 并投递成功；API 已核对 DB 持久化，重启后3条投递仍在。没有提供未实现目标 API 的假响应。

## 实际路由清单

下表由已锁定 controller 的 mapping 注解抽取，角色来自 SaTokenConfig 前缀规则；不替代 Service 对象归属校验。原始机器清单在 `upstream-routes.json`。

| 方法 | 上游路径 | 角色边界 | 固定源码位置 |
| --- | --- | --- | --- |
| GET | `/api/admin/audit/hr/list` | ADMIN | AdminAuditController.java:28 |
| POST | `/api/admin/audit/hr/decision` | ADMIN | AdminAuditController.java:33 |
| GET | `/api/admin/audit/job/list` | ADMIN | AdminAuditController.java:39 |
| POST | `/api/admin/audit/job/decision` | ADMIN | AdminAuditController.java:44 |
| GET | `/api/admin/audit/job/detail` | ADMIN | AdminAuditController.java:50 |
| GET | `/api/admin/user/page` | ADMIN | AdminUserController.java:27 |
| POST | `/api/admin/user/status/update` | ADMIN | AdminUserController.java:39 |
| POST | `/api/admin/user/delete` | ADMIN | AdminUserController.java:45 |
| POST | `/api/public/auth/login/password` | 公开 | AuthController.java:28 |
| POST | `/api/public/auth/login/email` | 公开 | AuthController.java:33 |
| POST | `/api/public/auth/register` | 公开 | AuthController.java:38 |
| POST | `/api/public/auth/code` | 公开 | AuthController.java:44 |
| POST | `/api/common/auth/logout` | 已登录 | AuthController.java:50 |
| GET | `/api/health` | 公开 | HealthController.java:12 |
| GET | `/api/hr/job/list` | HR | HrJobController.java:33 |
| POST | `/api/hr/job/save` | HR | HrJobController.java:38 |
| POST | `/api/hr/job/delete` | HR | HrJobController.java:44 |
| POST | `/api/hr/job/remove` | HR | HrJobController.java:50 |
| POST | `/api/hr/job/tags/save` | HR | HrJobController.java:56 |
| GET | `/api/hr/job/tags` | HR | HrJobController.java:62 |
| GET | `/api/hr/job/applications` | HR | HrJobController.java:67 |
| POST | `/api/hr/job/apply/status` | HR | HrJobController.java:72 |
| GET | `/api/hr/resume/file/download` | HR | HrJobController.java:78 |
| GET | `/api/public/job/list` | 公开 | JobBrowseController.java:20 |
| GET | `/api/public/job/detail` | 公开 | JobBrowseController.java:32 |
| GET | `/api/user/job/recommend` | USER | JobBrowseController.java:37 |
| GET | `/api/public/notice/list` | 公开 | NoticeController.java:27 |
| GET | `/api/admin/notice/list` | ADMIN | NoticeController.java:32 |
| POST | `/api/admin/notice/save` | ADMIN | NoticeController.java:37 |
| POST | `/api/admin/notice/delete` | ADMIN | NoticeController.java:43 |
| GET | `/api/public/tag/list` | 公开 | TagController.java:28 |
| GET | `/api/public/tag/search` | 公开 | TagController.java:33 |
| GET | `/api/admin/tag/list` | ADMIN | TagController.java:42 |
| GET | `/api/admin/tag/page` | ADMIN | TagController.java:50 |
| POST | `/api/admin/tag/save` | ADMIN | TagController.java:61 |
| POST | `/api/admin/tag/delete` | ADMIN | TagController.java:67 |
| POST | `/api/user/job/apply` | USER | UserApplyController.java:27 |
| GET | `/api/user/apply/list` | USER | UserApplyController.java:33 |
| GET | `/api/user/apply/list/detail` | USER | UserApplyController.java:38 |
| POST | `/api/user/apply/resume/update` | USER | UserApplyController.java:43 |
| GET | `/api/common/user/me` | USER | UserController.java:29 |
| GET | `/api/common/user/profile` | USER | UserController.java:34 |
| POST | `/api/common/user/profile/update` | USER | UserController.java:39 |
| POST | `/api/common/user/password/change` | USER | UserController.java:45 |
| POST | `/api/common/user/avatar/upload` | USER | UserController.java:51 |
| GET | `/api/user/resume/list` | USER | UserResumeController.java:32 |
| GET | `/api/user/resume/detail` | USER | UserResumeController.java:37 |
| POST | `/api/user/resume/save` | USER | UserResumeController.java:42 |
| POST | `/api/user/resume/delete` | USER | UserResumeController.java:48 |
| POST | `/api/user/resume/set-default` | USER | UserResumeController.java:54 |
| POST | `/api/user/resume/tags/save` | USER | UserResumeController.java:60 |
| POST | `/api/user/resume/file/upload` | USER | UserResumeController.java:66 |
| GET | `/api/user/resume/file/download` | USER | UserResumeController.java:74 |
| POST | `/api/user/resume/file/delete` | USER | UserResumeController.java:79 |
