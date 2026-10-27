# M1 迁移与字段字典

基线 `2494b0b51f7f65264838a0729ca2876f561ff515`。增量 SQL：`backend/sql/migrations/V001__m1_foundation.sql`，不含 CREATE DATABASE、USE、DROP 或 TRUNCATE；只对明确选择的空库上游 schema 或锁定上游 schema 应用。`scripts/m1/migrate.py` 限制连接本项目 socket/13306 且仅接受 `graduate_job_match_m1_*` 库名，拒绝重复/部分迁移。迁移前检查库身份并备份；MySQL DDL 隐式提交，迁移中断不能靠事务自动回滚，需恢复到新的隔离库后再核对。已有 M0 运行库没有原地升级。

空库方法是先执行经过审阅的上游 `backend/sql/init.sql` 的表定义（执行前确认并替换其 CREATE DATABASE/USE），再执行 V001；`scripts/m1/setup_test_databases.py` 为测试创建随机后缀隔离库并执行此过程。该脚本会建立三个新合成库和针对运行库的本地 app 用户授权；每次运行创建新的库，不覆盖、不删除旧库。

## 字段

| 表 / 字段 | 含义和空值 |
| --- | --- |
| sys_user.email_verified | 1 已验证、0 未验证；旧记录统一0，demo注册0，实际邮箱验证码注册成功为1 |
| student_profile.user_id | 用户主键外键及唯一键；只从会话取得 |
| student_profile.major_code | 专业代码，空为未知；学校沿用 sys_user.school，仅展示 |
| education_rank | 专科1、本科2、硕士3、博士4，空为未知 |
| graduation_year | 四位年份，空为未知 |
| expected_category_id | 意向类别，空表示未设置；写入时验证 job_category |
| salary_min | 税前月薪元整数，空/0 为无有效期望 |
| profile_version | 新建1，画像更新递增；旧 common 接口改学校也递增 |
| student_city_preference | user_id + city_code 唯一、user_id 外键；空集合为无偏好 |
| job_category | id、唯一 name；管理员新增，公开只读 |
| resume.version | BIGINT 默认1；简历内容/技能及附件修改使用版本；默认切换对其他简历的版本影响尚未形成完整并发验收 |
| job.category_id / city_code | 岗位标准类别和城市代码，旧文本字段保留，旧数据不猜测映射 |
| job.salary_min / salary_max / salary_negotiable | 税前月薪整数/面议；面议时薪资应为 NULL；旧字符串仅展示 |
| job.education_min_rank | 最低学历，NULL 不限 |
| job.graduate_year_min / max | 毕业年份闭区间，NULL 表示该边无限制 |
| job.accept_fresh_graduate | 1 接受、0 不接受、NULL 旧数据未知；个性化推荐只取1 |
| job.deadline | UTC DATETIME，NULL 无期限，过期岗位所有公共路径不可见；页面以北京时间展示 |
| job.version | BIGINT 默认1；编辑、标签变化、关闭、审批增加版本 |

旧学历只从最低 id 的有效默认简历精确映射：专科/大专→1，本科→2，硕士→3，博士→4；其他值、无有效默认简历均 NULL。多个有效默认简历时选最低 id，其余不删除；应由用户核实画像。原 resume.education 不再是新画像的评分权威来源。

## 验证与恢复

`docs/evidence/M1/migration-handoff.log` 实测空库、合成上游数据升级、重复运行拒绝；测试了本科→2、未知→NULL。`scripts/m1/restore_test.py` 使用 mysqldump `--single-transaction --skip-add-drop-table --skip-lock-tables --no-tablespaces` 备份当前合成库到 `.local/m1-backups/`，只创建全新 `graduate_job_match_m1_restore_<随机>` 库并恢复；本次核对6表行数一致，见 restore-test.log。备份含数据及散列，已被 Git 忽略，不能提交。该脚本专用于本机 demo，真实升级须另行制定经授权的备份、恢复与核对流程。

M1 未新增 job_apply 唯一键、快照、行为表、事件表或推荐批次，不会擅自清理原有重复投递。无法证明旧数据业务真实性；M3 前须处理历史重复数据和附件引用。
