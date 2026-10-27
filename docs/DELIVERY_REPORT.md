# 毕业生就业推荐与岗位匹配系统：交付报告

状态：**待统一审核**。代码位于 `/Users/guanghao/private/graduate-job-match`，私有仓库 `https://github.com/jackyalana/graduate-job-match` 的 `delivery/final` 分支；完整交付 SHA 见交接消息。基线为已审核 M2 `7d48c03cfd2de5944cf553e88af020894df332a5`，保留 M0/M1/M2 分支。

## 已实现

- 学生收藏、取消、隐藏、恢复和去重浏览；企业岗位审核、投递记录和状态操作；管理员基础统计。管理员的 HR/岗位审核及账号、标签操作记录操作者、时间和可提供的理由。推荐从后端真实计算，支持内容、协同过滤和热度融合，不使用随机分数或前端 mock。
- 每学生每岗位唯一投递，以数据库唯一约束和岗位行锁兜底；投递时保存画像、简历、技能字典、岗位、岗位技能、匹配和附件快照。允许修改当前简历及附件，不改变已投递快照；岗位下架保留投递。HR 附件下载检查岗位归属，学生只能下载本人投递快照。
- 状态：已投递、已查看、面试中、拒绝、录用、接受、婉拒、撤回。状态变更使用版本条件更新，并在同事务记录时间线；旧的变更投递简历入口返回 409，不能绕过快照。
- 协同过滤使用最近 90 天的去重浏览、有效收藏和投递；至少 3 个不同岗位兴趣、邻居至少 2 个共同岗位，最多 20 位邻居。热度统计 30 天内去重收藏和投递；无邻居按 CONTENT_HOT 回退，无内容和邻居按 HOT_FALLBACK。算法版本 `hybrid-v1`。

## 启动与演示

此机器的演示页面为 `http://127.0.0.1:15174/`，后端 `http://127.0.0.1:18081`。账号 `demo_admin`、`demo_hr`、`demo_student` 等及随机密码仅在本机项目根目录通过 `python3 scripts/m0/local.py accounts` 获取，不写入仓库或录像。默认合成数据，真实 SMTP、模型 API 和招聘站不参与核心流程。

首次在本机重建需先按 [基线说明](BASELINE.md) 准备项目专用 `.local` JDK、Maven、MySQL、随机凭证与前端依赖；不得操作真实业务库。在项目根目录，确认旧演示进程后：

```bash
python3 scripts/m1/stop.py
# 仅当项目专用 MySQL 未运行时执行：python3 scripts/m0/local.py db-start
python3 scripts/delivery/setup.py
JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package
python3 scripts/m1/start.py
python3 scripts/m1/frontend.py
```

`setup.py` 每次创建独立全新合成库，不清理旧库；普通演示不应执行会修改数据的测试脚本。HR 登录发布岗位，管理员审核通过；学生填写画像和简历后浏览、收藏或推荐并投递；HR 在岗位管理中查看投递、邀请面试并发出录用；学生在投递记录中接受，三方可查看状态或统计。当前演示库中已有合成的已接受记录，也可创建新岗位重演。

主要增量接口：学生 `POST /api/student/jobs/{id}/actions/{FAVORITE|HIDE}?active=true|false`、`POST /api/student/jobs/{id}/view`、`GET /api/student/job-actions`；投递使用旧 `POST /api/user/job/apply`，学生及企业状态更新分别为 `POST /api/user/apply/status`、`POST /api/hr/job/apply/status`，请求体 `{applyId,status,version}`；时间线分别为 `GET /api/user/apply/events?applyId=`、`GET /api/hr/job/apply/events?applyId=`，学生与对应 HR 分别通过 `/api/user/apply/attachment` 与 `/api/hr/resume/file/download` 鉴权获取快照。`GET /api/admin/statistics` 仅管理员。状态号 0–7 依次为已投递、已查看、面试中、拒绝、录用、接受、婉拒、撤回。既有推荐批次接口继续使用，算法版本变更会使旧批次失效。

## 增量迁移与历史数据

`backend/sql/migrations/V003__delivery_workflow.sql` 新增投递版本和快照字段、学生/岗位唯一约束、状态事件、收藏/隐藏和行为事件表；`V004__audit_log.sql` 增加管理员审计日志。演示专用 `scripts/delivery/migrate.py`、`audit_migrate.py` 仅允许项目本机 13306 的指定隔离库，先检查前置结构与重复学生/岗位记录；发现重复或部分迁移时拒绝，不删除历史。MySQL DDL 非原子，事前备份，失败后在隔离库按备份恢复，不能盲目重复。

旧版投递没有投递时快照，无法追溯重建：迁移保留记录并标记快照不可用，界面不把当前简历或岗位伪称为历史快照；旧附件仍需对应企业鉴权且不可替换。真实历史数据的恢复与迁移方案需另行制定。本次在合成旧库验证迁移保留和提示，V003 的备份恢复未实际演练。

## 实际验证和实验

所有测试均在本机隔离合成库执行：后端 `package` 17 项 JUnit、0 失败/跳过；前端 `npm run build` 成功，有 Vite chunk 大小警告。最终业务回归 `scripts/delivery/m3_test.py` 46 次实际 HTTP 请求，含并发重复投递、并发状态冲突、跨学生/企业、简历及岗位快照不变、岗位下架保留及审核→投递→面试→录用→接受；`scripts/delivery/m4_test.py` 45 次实际请求，含协同过滤、收藏取消、隐藏恢复、批次失效和统计；管理员审计验证 13 次请求。记录与截图见 [证据目录](evidence/delivery/)。

固定种子 `20261027` 的合成评估由 `python3 scripts/delivery/evaluate.py` 两次运行结果一致；60 用户、80 岗位，按时间段 70/15/15 切分，验证集不参与评分，测试正反馈不进入训练；10 人为无训练行为冷启动组。结果及源码提交、数据哈希、分组指标见 [实验 manifest](../evaluation/output/synthetic-20261027/manifest.json)、[指标 JSON](../evaluation/output/synthetic-20261027/metrics.json) 和 CSV。

| 方法 | P@10 | Recall@10 | NDCG@10 |
| --- | ---: | ---: | ---: |
| HOT | 0.0283 | 0.1417 | 0.0773 |
| CONTENT | 0.1700 | 0.8500 | 0.6113 |
| HYBRID | 0.1717 | 0.8583 | 0.6269 |

合成偏好和岗位类别有意相关，标签不是独立真实用户偏好；上述数值只能说明该模拟设置，不能声称真实准确率、就业率或满意度。未使用真实学生数据。

目标规模性能检查使用 1,000 合成学生、2,000 岗位、20,000 行为、10 并发、预热 30 秒和采样 60 秒。Apple M5、32 GiB、Zulu JDK 8u512、MySQL 8.4.11 的本次结果：普通列表 P95 17.21 ms、批次创建 P95 109.43 ms、批次页 P95 98.53 ms，采样错误率 0；细节见 `evidence/delivery/performance.json`。这是本机合成负载一次测量，不代表生产容量；测试时评分代码与交付一致，随后调整历史记录展示、快照字典捕获、隐藏原因和管理员审计，未再次进行整套性能采样。

## 已知限制

- 原项目及其 MIT 许可证、来源记录保留；AI 辅助和改造内容须在论文中如实说明，不能包装为全原创。
- 老投递没有真实历史快照，V003 不伪造；上游历史数据重复时迁移中止，需人工审计处理。实验只覆盖合成数据；无真实用户研究。
- 管理统计当前为累计投递和当前公开在招岗位，不支持日期筛选；无多实例压测，旧框架未做生产安全评估。别名维护无管理页面。七天批次清理未等待实际周期验证。
- 全新安装依赖项目本机工具准备，不自动公开部署。未验证 Docker Compose、真实 SMTP、Redis、移动端或外部服务。
