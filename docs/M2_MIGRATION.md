# M2 迁移和恢复说明

基于已完成 V001 的 M1 数据库执行 `backend/sql/migrations/V002__m2_recommendation_batches.sql`，新增 `recommendation_batch`、`skill_tag_alias`，不改动既有岗位、简历及投递数据。两张表初始为空。SQL 无 CREATE/USE/DROP 数据库和 TRUNCATE。MySQL DDL 非原子，操作前应备份；失败后先检查已创建的表，不能盲目重复。

本机演示专用 `python3 scripts/m2/migrate.py graduate_job_match_m1_...` 只接受本项目命名、项目本机 MySQL 13306、完整 M1 结构及两张新表均不存在的库，重复或部分执行拒绝。`python3 scripts/m2/setup.py` 在项目私有 MySQL 新建三套隔离合成 M1 库，对 runtime 应用 V002，不清理旧库。空库和上游结构升级后执行 V002、重复执行拒绝的结果在 `evidence/M2/migration-cases.log`。生产/真实库不在本脚本权限范围内。

恢复须使用事前完整备份恢复到新隔离库，验证应用及行数后再由负责人决定切换；不对已有库自动回滚或清理。批次记录只含合成演示岗位结果，本地 `.local` 数据库和备份不提交。应用每小时清理已过期且创建超过七天的推荐批次；本次未等待七天验证定时清理。

上游 `job_tag.weight` 是 `tag.base_weight * proficiency`（常见 3、6、9），不是 SDD 的独立重要度。M2 不把它直接作为重要度：技能要求等级使用 `job_tag.proficiency`，重要度使用 `tag.base_weight` 并限制在 1–3（缺失为 1，小于 1 映射 1，大于 3 映射 3）；学生等级使用 `resume_tag.proficiency`。审核修复后，无效岗位技能等级和损坏的岗位标签引用返回 409 `TAG_DATA_INVALID`，新增技能等级必须为 1–3；学生历史无效等级仍不作为有效技能。映射不改旧记录，其他标签类型不进入技能评分。权重含义仍需论文说明。

`skill_tag_alias` 将别名标签 ID 映射到标准技能 ID；链式别名最终归一到启用的技能，循环或无效标准技能返回 409 拒绝计算。同一标准技能重复出现时取最高等级及重要度。管理员维护标签时引用保护覆盖别名两端以及岗位、简历引用。当前没有别名管理页面或接口；授权的直接数据库维护须在同一事务锁定涉及的标签并验证链的完整性，不能绕过应用直接并发修改。别名表为空时，各标准 tag ID 自身就是规范 ID。
