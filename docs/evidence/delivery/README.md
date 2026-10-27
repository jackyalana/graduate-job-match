# 交付验证证据

全部为项目本机隔离合成数据。原 M0/M1/M2 证据保留；本目录也保留中间失败与后续修正日志，不把失败当通过。构建采用本机项目私有 JDK/Maven，Node 和 npm 环境见交付报告。

| 项目根目录命令 | 退出码及证据 |
| --- | --- |
| `JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package` | 0；backend-release.log，17 项 JUnit，无失败或跳过 |
| `cd frontend && VITE_API_BASE_URL=http://127.0.0.1:18081 VITE_DEMO=true npm run build` | 0；frontend-release.log，Vite chunk 警告 |
| 新建隔离库并启动后 `python3 scripts/delivery/m3_test.py` | 0；http-release-m3.log/http-release-m3.json，46 次 HTTP 请求，以及数据库唯一性、审计和快照字段断言 |
| 另建隔离库并启动后 `python3 scripts/delivery/m4_test.py` | 0；http-release-m4.log/http-release-m4.json，45 次 HTTP 请求 |
| 在合成库 `python3 scripts/delivery/audit_test.py` | 0；audit-http.log/audit-http.json，13 次 HTTP 请求和审核原因、标签审计、越权断言 |
| `node scripts/m2/recommend_race_test.cjs` | 0；frontend-race.log，保留 M2 筛选请求竞态回归 |
| `python3 scripts/delivery/evaluate.py` | 0；evaluation.log；固定 seed 的输出及源码提交和哈希位于 `evaluation/output/synthetic-20261027/` |
| 新建隔离库并启动后 `python3 scripts/delivery/performance.py` | 0；performance.log/performance.json；10 并发、30 秒预热、60 秒采样，1,000/2,000/20,000 合成规模 |
| 新 M2 合成库插入一条旧投递、依次执行 V003/V004 迁移后运行 `python3 scripts/delivery/legacy_test.py` | 0；legacy-release.log，旧投递保留且明确标记快照不可用；迁移重复执行被拒绝 |
| 新 M2 合成库插入同人同岗位的两份旧投递并执行 V003 迁移 | 0；duplicate-migration.log，迁移拒绝且两条记录保留 |

浏览器实际检查学生接受记录和时间线 `student-accepted.png`、企业下架岗位后的历史记录 `hr-history.png`、管理员统计 `admin-statistics-final.png`。截图取自交付过程中的合成场景；最终 API 回归以上述日志为准。早期 `m4-http.log` 是行为时间精度修正前的失败记录，`m4-http-final.log` 和 `http-delivery-m4.log` 为修复后的实际结果。

测试脚本会写入和改变演示数据，仅对每次新建的隔离合成库运行；不要对真实库或现有展示数据重复执行。性能检查结束后已切回独立演示库。
