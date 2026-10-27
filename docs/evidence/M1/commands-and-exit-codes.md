# M1 命令、环境与退出码

项目根：`/Users/guanghao/private/graduate-job-match`；macOS 26.6 arm64、Zulu JDK 8.0.512、Maven 3.9.11、Node 26.8.2/npm 11.19.1、MySQL 8.4.11。证据文件中每个退出码由实际 shell 记录。`http-results.json` 是对应 M1 真实 HTTP 请求的脱敏状态、code 和 requestId 校验结果，非数据库 dump。

| cwd | 命令 | 退出码 | 证据与范围 |
| --- | --- | --- | --- |
| 根 | `JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package` | 0 | backend-package-handoff.log；11原有JUnit测试，0失败0跳过，随后JAR打包 |
| frontend | `VITE_API_BASE_URL=http://127.0.0.1:18081 VITE_DEMO=true npm run build` | 0 | frontend-build-verified.log；Vite chunk大小警告仍存在 |
| 根 | `python3 scripts/m1/setup_test_databases.py` | 0 | migration-handoff.log；新建独立空库、合成上游库及运行库；重复迁移拒绝（被测试的子进程exit1为预期） |
| 根 | `python3 scripts/m1/start.py` | 0 | start-handoff.log；127.0.0.1:18081，项目专属MySQL 13306 |
| 根 | `python3 scripts/m1/http_tests.py` | 0 | http-tests-handoff.log + http-results.json；128次真实HTTP请求及数据库断言 |
| 根 | `python3 scripts/m1/normal_registration_test.py` | 0 | normal-registration-handoff.log；独立18082 dev配置拒绝无验证码及固定 000000 验证码 |
| 根 | `python3 scripts/m1/test_stop_identity.py` | 0 | stop-identity-test.log；身份不匹配的自有测试进程未被信号终止 |
| 根 | `python3 scripts/m1/stop.py` | 0 | stop-pre-final.log；核验 PID、启动时间、命令和进程组后停止专属前后端 |
| 根 | `python3 scripts/m1/restore_test.py` | 0 | restore-test.log；合成库备份恢复到全新库，6表行数一致；备份只在.local |
| 根 | `node --input-type=module -e "import {formatShanghai} from './frontend/src/utils/time.js'; if(formatShanghai('2026-10-27 02:55')!=='2026-10-27 10:55') process.exit(1); console.log('PASS UTC 02:55 -> Asia/Shanghai 10:55');"` | 0 | time-test.log；固定UTC时间转换为北京时间 |
| 根 | `python3 scripts/m1/frontend.py` | 0 | frontend-start-handoff.log；M1 Vite 127.0.0.1:15174，显式VITE_DEMO |

初期 compile-first.log 因 Sa-Token `NotLoginException.newInstance` 参数个数不符退出1；已修复并完成后续全量测试。先前多轮打包、迁移、HTTP和浏览器日志属于中间版本；以表格中列出的日志和 `http-results.json` 为最终候选的证据。浏览器截图标记中间/最终版本见 browser-observations.md。没有在真实数据库、真实 SMTP 或收费服务上运行。
