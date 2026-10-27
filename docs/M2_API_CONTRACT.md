# M2 接口契约（待审核）

所有接口使用 M1 的 `satoken` header、真实 HTTP 状态与 requestId，下面接口要求 USER。默认简历为本人的有效默认简历；传入 `resumeId` 必须属于本人且有效，否则 404 或业务错误。未提供有效默认简历时允许资料不足的热度兜底。

| 方法和路径 | 输入与输出 |
| --- | --- |
| `POST /api/student/recommendation-batches` | 可选 JSON：`resumeId, keyword, city, jobType, tagIds, categoryId, salaryMin, salaryMax`。按筛选条件和资格读取全部候选并评分，返回 `batchId, algorithmVersion, expiresAt, totalEligible, totalRanked, page, pageSize, items, missingProfileFields`，首屏 10 条。 |
| `GET /api/student/recommendation-batches/{id}?page=1&pageSize=10` | 仅本人；`page>=1`、`1<=pageSize<=50`；固定批次排序，返回同样分页结构；过期、输入变化或资格变化返回 409 `BATCH_STALE`，其他用户批次返回 404 `BATCH_NOT_FOUND`。 |
| `GET /api/student/matches/{jobId}?resumeId=` | 岗位须公开可见；符合个人资格返回匹配详情，不符合返回 `eligible:false`、实际 `reasons` 和 `missingProfileFields`；不可见岗位 404。 |
| `GET /api/user/job/recommend` | 原参数和旧列表结构保留，调用同一 M2 候选及排序逻辑；`matchScore` 现在为 0–100 或 null（M1 的旧标签分为 0–1）。它是无状态入口，每次重新计算；跨请求稳定分页请使用批次接口。 |
| `GET /api/hr/job/applications?jobId=` | 对应岗位 HR；当前有效简历的 `matchScore` 使用相同内容公式、0–100 或 null；历史快照在 M3。 |

批次项包含岗位字段和 `match`：`matchScore`、`recommendationScore`（均 0–100，一位小数）、`algorithmVersion`、`strategy`、`components`、`effectiveWeights`、`dimensions`、`dimensionWeights`、`matchedSkills`、`missingSkills`、`missingProfileFields`、`reasons`、`fallbackReason`。各分项四位小数，排序使用未舍入值。没有内容维度时匹配分为 null，策略 `HOT_FALLBACK`，只用热度排序。已填写而不匹配为 0。不可见岗位不因旧接口进入结果。

资格原因：`FRESH_GRADUATES_NOT_ACCEPTED`、`EDUCATION_REQUIREMENT_NOT_MET`、`GRADUATION_YEAR_REQUIREMENT_NOT_MET`、`ALREADY_APPLIED`；缺失学历或毕业年放在 `missingProfileFields`。可见性失效时返回 404，不暴露隐蔽岗位详情。

审核修复后的 `tagIds` 规则：缺省、null、空串或全空格表示不筛选；非空值必须是以英文逗号分隔的正整数，每项可有首尾空格，但禁止空元素、非数字、零、负数及超出 64 位范围。新批次、旧推荐及公共岗位列表共享校验，非法返回 422 `VALIDATION_FAILED`。

管理员删除、停用或修改标签类型时，如果该标签被岗位、简历、别名来源或标准节点引用，返回 409 `TAG_IN_USE`，包含可读原因；别名链的中间节点也受保护。名称和权重等安全更新仍可进行。技能等级必须为 1–3；检测到历史数据损坏或无效别名时明确返回 409 `TAG_DATA_INVALID`，不会静默丢弃岗位技能要求。
