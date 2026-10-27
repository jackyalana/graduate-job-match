# M2 审核修复验证

相对 `e48ac28dc1e0c7794b65431c482097ed9b8d3237`；原 M1/M2 日志与结果文件保留。测试仅使用项目本机隔离 MySQL 13306、合成数据库、后端 18081、前端 15174。环境为 macOS、Zulu JDK 8u512、Maven 3.9.11、Node 26.8.2、npm 11.19.1、MySQL 8.4.11。

| 工作目录 | 实际命令 | 退出码及结果 |
| --- | --- | --- |
| 项目根 | `JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package` | 0；backend-package-handoff.log，16 项测试，0 失败、0 跳过 |
| frontend | `VITE_API_BASE_URL=http://127.0.0.1:18081 VITE_DEMO=true npm run build` | 0；frontend-build-final.log，有 chunk 大小警告 |
| 项目根 | `node scripts/m2/recommend_race_test.cjs` | 0；recommend-race-final.log。受控旧成功、旧失败和旧 finally 乱序返回，以及旧分页刷新；断言新批次 ID、列表、游标和加载状态 |
| 项目根 | 新库 `python3 scripts/m2/setup.py`，启动后 `python3 scripts/m2/regression.py` | 0；m2-regression-last.log/m2-results-last.json，49 次真实 HTTP 请求；运行时原 M2 结果文件恢复原样 |
| 项目根 | 上述数据准备后 `python3 scripts/m2/review_regression.py` | 0；review-http-last.log/review-http-results-last.json，73 次真实 HTTP 请求。包括 `,`、`,,`、`1,,2`、非数字、空白和合法列表；别名来源、链式中间节点和标准标签的删除、停用及改类型；安全更新后推荐、详情、HR 匹配及非法数据明确 409 |
| 项目根 | 另一全新库中运行 `python3 scripts/m1/http_tests.py` | 0；m1-regression-handoff.log/m1-results-handoff.json，128 次真实 HTTP 请求；运行时原 M1 结果文件恢复原样 |

浏览器使用 `scripts/m2/delay_proxy.py` 在 127.0.0.1:18083 将上海批次创建和旧批次第 2 页各延迟 15 秒；前端临时指向该代理。真实操作上海筛选后、请求未完成时改为北京并再次查询，旧请求结束后仍只显示北京，见 `beijing-after-switch.png`、`beijing-after-old-response.png` 和 `browser-switch-proxy.log`。另外触发旧批次第 2 页后立即刷新北京，旧分页结束后仍是新批次列表，见 `refresh-after-old-page.png` 和 `browser-proxy.log`。这些截图运行在相同前端代码和此前后端构建；最终后端构建的接口回归见上表。浏览器不会展示批次 ID/游标，相关状态由受控乱序测试断言。

测试代理和临时前端已关闭，普通前端与后端已恢复。演示地址 `http://127.0.0.1:15174/`，账号通过项目根 `python3 scripts/m0/local.py accounts` 获取。别名表仍无管理 API；直接数据库维护必须按迁移说明持锁并验证，不能并发绕过这些保护。
