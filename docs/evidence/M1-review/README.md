# M1 审核修复验证（待审核）

相对提交 `690d7d0d7d5f527f0b3b17a7ce0de45a5b9993d3`，保留 `docs/evidence/M1/` 的原始证据。仅使用本机 MySQL 13306 新建合成库及 127.0.0.1:18081 / 15174。账号和密码通过 `python3 scripts/m0/local.py accounts` 在本地获取，勿复制进仓库。未访问 SMTP 或真实数据库。

| 工作目录 | 命令 | 退出码和结果 |
| --- | --- | --- |
| 项目根目录 | `JAVA_HOME="$PWD/.local/jdk" .local/maven/bin/mvn -B -o -f backend/pom.xml -Dmaven.repo.local="$PWD/.local/m2" package` | 0，backend-build.log；11 测试、无失败/跳过 |
| frontend | `VITE_API_BASE_URL=http://127.0.0.1:18081 VITE_DEMO=true npm run build` | 0，frontend-build.log；有 chunk 大小警告 |
| 项目根目录 | `python3 scripts/m1/setup_test_databases.py`；`python3 scripts/m1/start.py` | 0，migration.log/start.log；新合成库 |
| 项目根目录 | `python3 scripts/m1/review_regression.py` | 0，http-regression.log / http-results.json；31 次 HTTP 请求，断言用户名和邮箱大小写及重音变体、岗位条件与标签、陈旧版本、三类参数错误 |
| 项目根目录 | 再次创建新库并运行 `python3 scripts/m1/http_tests.py` | 0，full-migration.log/full-start.log/full-http.log/full-http-results.json；128 次 HTTP 请求；原结果文件在运行后原样恢复 |

浏览器实际操作：HR 登录，打开已有合成岗位标签并保存，列表立即显示待审核；紧接着编辑名称并保存，显示成功和待审核（hr-edit-success.png）。管理员进入审核详情，看到结构化薪资、学历、毕业年、应届、截止时间、Java 技能标签及版本（admin-audit-detail.png）。审核页面按钮以详情里的版本提交；接口回归另外验证了旧版本 409 和查看版本通过。截图均为合成数据。

本次没有做真实邮件发送、跨进程分布式限流或 M2/M3 功能测试。未知账号的拼写变体只做小写归一，已存在账号以数据库识别的 ID 归一。页面类别目前显示类别 ID、学历显示等级；对照字段字典解释。
