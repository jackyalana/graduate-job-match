# M1 接口契约与旧入口映射

基线后增量；当前代码和 `http-results.json` 为准。文中目标接口之外的 M2/M3 接口未完成。响应：成功 `{"code":"OK","message":"OK","data":...,"requestId":"...","details":null}`；错误带 `code/message/requestId/details`，不返回堆栈。`X-Request-Id` 与 JSON requestId 对应；文件下载提供同名响应头。401 未认证/停用、403 角色不符、404 不存在或对象不可见、409 冲突/旧版本、413 超大小、422 参数/类型不合法、429 限流；部分现有业务拒绝如不可投递、失效简历仍返回HTTP400，见实际测试，不能声称所有目标错误均已归一。

认证只从 `satoken` header；不读 Cookie。`/api/common/**` 属于有效登录的 USER、HR、ADMIN 三角色，并非仅 USER；每次请求核查账户及 HR 审核状态。`/api/user/**`、`/api/student/**`=USER，`/api/hr/**`=已审核启用的HR，`/api/admin/**`=ADMIN。`/api/public/**`、`GET /api/jobs`、`GET /api/jobs/{id}`、health公开；岗位详情服务仍执行统一可见性检查。

| 方法 / 路径 | 权限与行为 |
| --- | --- |
| POST /api/public/auth/register | USER/HR；仅 demo profile + 明确开关 + 本机来源允许免验证码并标识未验证，普通配置须真实邮箱码，不能注册管理员 |
| POST /api/public/auth/login/password | 用户名密码；单实例按直接客户端+用户名限流 |
| POST /api/common/auth/logout；GET /api/common/user/me | 三角色本人；账号停用后旧 token 不可用，me 不含密码、包含 emailVerified |
| GET / PUT /api/student/profile | USER 本人；school、majorCode、educationRank、graduationYear、expectedCategoryId、salaryMin、cityCodes；服务端决定userId，拒绝额外字段，更新 profileVersion |
| GET /api/public/job/categories；POST /api/admin/job/categories | 公开列表；管理员新增 name |
| GET /api/jobs | 公开，keyword、city、jobType、tagIds、categoryId、salaryMin/max、page、pageSize；返回 items/total/page/pageSize |
| GET /api/jobs/{id} | 公开，只展示审核通过、在招、未过期且企业启用且已审核岗位 |
| GET /api/public/job/list, /api/public/job/detail | 旧路径调用同一规则；list支持类别和薪资区间，旧分页为 cursorId/pageSize |
| GET /api/user/job/recommend | USER；增加相同类别/薪资/城市筛选，限应届且学历/毕业年符合、排除已投递；details.missingProfileFields 提醒缺少学历/毕业年；评分仍是上游基线，M2尚未完成 |
| POST /api/hr/job/save；POST /api/hr/job/tags/save | 本人HR；结构化条件、版本；更新和标签变更重新审核。已有岗位更新必须提供当前 version |
| GET /api/admin/audit/job/list；POST /api/admin/audit/job/decision | ADMIN；列表返回version，决策提交 id/pass/version，旧版本或非待审返回409 |
| POST /api/user/resume/file/upload；POST /api/student/resumes/{id}/attachment | USER本人；multipart file，PDF扩展名/MIME/文件头，<=5MiB |
| GET /api/user/resume/file/download?resumeId=；GET /api/student/resumes/{id}/attachment | USER本人；无裸静态简历链接 |
| GET /api/hr/resume/file/download?applyId=；GET /api/hr/applications/{id}/attachment | 仅对应岗位HR及实际投递；跨HR返回404；当前为引用简历附件，历史文件快照在M3 |
| POST /api/user/resume/delete；POST /api/hr/job/remove | 本人简历归档或本人岗位关闭，保留历史投递 |
| POST /api/admin/user/delete | 暂拒绝409，避免物理删除关联投递；账号可停用 |
| POST /api/user/job/apply | 旧路径保留，验证本人有效简历和统一公开岗位有效性；重复用户/岗位并发唯一、快照及关闭竞争仍属M3 |

公开浏览与个人资格区分：任何人可查看有效岗位要求；推荐另检查应届标志及学历/年份，受限字段未知时排除并提示资料。当前投递检查公开有效性和本人有效简历，SDD没有明确把个人推荐专属过滤全部强加给投递，若要收紧需先有 ADR/审核。

审计范围以 128 次真实 HTTP 请求与 normal-registration-handoff.log 为证；没有宣称完整 OpenAPI 或尚未实现的未来接口。M0 文档仍保留为上游历史证据，common角色误标已修正。
