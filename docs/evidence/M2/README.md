# M2 验证证据

环境：项目根目录 `/Users/guanghao/private/graduate-job-match`；Zulu JDK 8u512、Maven 3.9.11、Node 26.8.2、npm 11.19.1、MySQL 8.4.11。MySQL `127.0.0.1:13306`、后端 `127.0.0.1:18081`、前端 `127.0.0.1:15174`。仅合成测试数据。未设置 `JAVA_HOME` 时系统 Maven 启动找不到 Java，本次打包命令显式设置项目私有 JDK。首次安装见 M0/M1 证据。日志保留不同中间阶段，以 handoff、最终 http 和本说明为交付依据。

| 目录 | 命令 | 退出码及证据 |
| --- | --- | --- |
| 项目根 | `JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package` | 0；backend-package-handoff.log，14 测试、0 失败、0 跳过 |
| frontend | `VITE_API_BASE_URL=http://127.0.0.1:18081 VITE_DEMO=true npm run build` | 0；frontend-build-handoff.log，chunk 大小警告 |
| 项目根 | `python3 scripts/m2/setup.py` | 0；setup-final.log，新建 M1 隔离合成库并应用 V002。空库与上游升级库迁移另见 migration-cases.log，重复子命令退出 1 为预期拒绝 |
| 项目根 | `python3 scripts/m1/start.py` | 0；start-final.log，使用项目 `.local/m1-schemas.json` 指向的 M2 runtime |
| 项目根 | `python3 scripts/m2/regression.py` | 0；http-regression.log、http-results.json，49 次实际请求及真实数据库断言；仅对新建空合成 runtime 运行一次 |
| 项目根 | 新建另一 M2 合成库，执行 `python3 scripts/m1/http_tests.py` | 0；m1-regression-handoff.log 和 m1-regression-handoff-results.json，128 次请求；执行后还原原 M1 证据文件及 M2 演示库指向 |

浏览器合成账号 `demo_student` 登录后查看匹配分、排序分与技能明细，见 handoff-recommendation.png、handoff-detail.png。`demo_student2` 的最终构建冷启动页面见 handoff-cold-fallback.png。截图没有真实学生资料。

当前演示地址 `http://127.0.0.1:15174/`；本机账号由项目根 `python3 scripts/m0/local.py accounts` 获取。重建演示数据前先执行 `python3 scripts/m1/stop.py backend`，再运行 setup 和 start；前端如未启动执行 `python3 scripts/m1/frontend.py`。脚本只用于项目隔离环境，不用于其他项目或真实库。
