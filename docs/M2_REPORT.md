# M2 内容匹配与冷启动交接

状态：**待审核**。从 M1 `daba2afeab4da305491bdf629503c66537e493e8` 创建 `m2/matching`；提交 SHA 和远程状态以交接消息为准。项目 `/Users/guanghao/private/graduate-job-match`；私有仓库 https://github.com/jackyalana/graduate-job-match。M0/M1 分支保留。

先将 M1 两处误写的 131 次原有 HTTP 回归改为 128 次，原日志与测试结果未改动。

| 内容 | 位置和结果 |
| --- | --- |
| 四维内容分、缺失与解释 | `matching/ContentScorer.java`、`RecommendationCalculator.java`、`MatchExplanation.java`；权重 0.50/0.20/0.15/0.15，算法版本 `content-hot-v1`；评分用未舍入值。 |
| 候选与热度 | `RecommendationServiceImpl`、`JobEligibilityServiceImpl`；全部符合条件岗位批量读取标签评分，之后排序截取 200；近 30 天投递按用户/岗位去重。收藏尚无实现，收藏数为 0。 |
| 批次、版本、权限 | `RecommendationController`、`RecommendationServiceImpl`、V002；结果持久化、10 分钟有效，本人读取；输入指纹和实时资格检查使变化的批次失效；重复行为的去重集合未变化时不使批次失效。每页最大 50。 |
| 兼容入口和页面 | `JobBrowseServiceImpl` 旧推荐转到同一公式；HR 当前简历分不再调用旧 `computeAndSave`；`Home.vue` 真实展示匹配分、排序分、原因及冷启动提示。详见 `M2_API_CONTRACT.md`。 |
| 迁移及映射 | `M2_MIGRATION.md`、`backend/sql/migrations/V002__m2_recommendation_batches.sql`；上游权重不直接充当重要度。 |

## 实际验证

命令和日志见 `evidence/M2/README.md`。最终后端打包含 14 项 JUnit、0 失败/跳过；黄金样例 C=0.825、匹配分 82.5、H=0.4 时排序分 74.0 由纯函数测试覆盖。前端 build 成功，有 chunk 大小警告。最终 M2 真实 MySQL 回归 49 次 HTTP 请求，含 205 个合格候选、旧岗位排序靠前、技能不相交及别名、学历和截止时间、已投递、固定分页、失效、跨用户拒绝、重复用户投递去重、8 个并发批次和 8 个并发 HR 申请列表，旧 `match_score` 写入数为 0。最终构建另重跑 M1 的 128 次请求。均使用隔离合成库。最终浏览器查看推荐、详情及冷启动，截图见证据目录。

## 阶段限制和偏离

- M2 不实施 CF、收藏、隐藏和行为事件，因此收藏人数为 0、没有 HIDE 数据；策略是 CONTENT_HOT 或 HOT_FALLBACK，不宣称评估效果。M3 完整状态机与投递快照、M4 算法实验未实施。
- 批次版本用持久化的输入 SHA-256 指纹而非数据库自增全局版本。读取页时批量重新读取计算输入并比较，交易内保持一致快照，动态截止和资格也参与校验；这换取单实例简单正确性，但分页仍要查询数据，并未验证 2,000 岗位性能。
- 别名数据可通过受控迁移维护，尚无管理页面；没有自动猜测同义词。数据不符合别名约束会拒绝计算。详见迁移说明。
- HR 申请列表逐申请者重新计算当前匹配，未进行大规模性能验证，也不代表投递时快照；归档简历或不符合资格时分数为空。初始上游 `MatchServiceImpl` 及其旧缓存代码仍留作历史，现有业务调用链已改用 M2，不将旧结果当 M2 分。
- 批次七天清理任务已配置，但未等待七天验证；未做多实例并发、性能压测、真实用户评估、邮件发送或外部服务调用。

**待审核。**
