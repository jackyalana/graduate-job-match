# 实际命令与退出码

完整机器记录：commands.jsonl。cwd相对项目根目录；command为JSON argv，实际命令不包含私有密码。

| UTC开始 | cwd | 命令argv | 退出码 | 原始日志 |
| --- | --- | --- | --- | --- |
| 2026-10-27T01:49:05.351766+00:00 | .local/downloads | `["curl", "-fL", "--retry", "2", "-o", "jdk.tar.gz", "https://cdn.azul.com/zulu/bin/zulu8.98.0.107-ca-jdk8.0.512-macosx_aarch64.tar.gz"]` | 0 | [download-jdk.log](download-jdk.log) |
| 2026-10-27T01:49:10.002601+00:00 | .local/downloads | `["curl", "-fL", "--retry", "2", "-o", "maven.tar.gz", "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz"]` | 0 | [download-maven.log](download-maven.log) |
| 2026-10-27T01:49:10.686695+00:00 | .local/downloads | `["curl", "-fL", "--retry", "2", "-o", "mysql.tar.gz", "https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-macos15-arm64.tar.gz"]` | 0 | [download-mysql.log](download-mysql.log) |
| 2026-10-27T01:49:40.208406+00:00 | frontend | `["npm", "ci", "--ignore-scripts", "--no-audit", "--no-fund", "--fetch-retries=0"]` | 0 | [frontend-install-retry.log](frontend-install-retry.log) |
| 2026-10-27T01:49:06.463869+00:00 | frontend | `["npm", "ci", "--ignore-scripts", "--no-audit", "--no-fund"]` | 1 | [frontend-install.log](frontend-install.log) |
| 2026-10-27T01:50:15.442200+00:00 | . | `["gh", "repo", "create", "jackyalana/graduate-job-match", "--private", "--description", "毕业生就业推荐与岗位匹配系统；基于 Lan-Recruitment 的注明来源二次开发"]` | 0 | [github-create.log](github-create.log) |
| 2026-10-27T01:50:25.123303+00:00 | backend | `["env", "JAVA_HOME=/Users/guanghao/private/graduate-job-match/.local/jdk", "../.local/maven/bin/mvn", "-B", "-Dmaven.repo.local=/Users/guanghao/private/graduate-job-match/.local/m2", "test"]` | 1 | [backend-tests.log](backend-tests.log) |
| 2026-10-27T01:50:25.470993+00:00 | frontend | `["env", "VITE_API_BASE_URL=http://127.0.0.1:18080", "npm", "run", "build"]` | 127 | [frontend-build.log](frontend-build.log) |
| 2026-10-27T01:50:35.638232+00:00 | frontend | `["npm", "ci", "--ignore-scripts", "--no-audit", "--no-fund", "--fetch-retries=0"]` | 0 | [frontend-install-final.log](frontend-install-final.log) |
| 2026-10-27T01:50:55.392947+00:00 | . | `[".local/mysql/bin/mysqld", "--no-defaults", "--initialize-insecure", "--basedir=/Users/guanghao/private/graduate-job-match/.local/mysql", "--datadir=/Users/guanghao/private/graduate-job-match/.local/mysql-data", "--default-time-zone=+00:00"]` | 2 | [mysql-initialize.log](mysql-initialize.log) |
| 2026-10-27T01:51:00.627217+00:00 | frontend | `["env", "VITE_API_BASE_URL=http://127.0.0.1:18080", "npm", "run", "build"]` | 0 | [frontend-build-retry.log](frontend-build-retry.log) |
| 2026-10-27T01:51:13.447768+00:00 | . | `[".local/mysql/bin/mysqld", "--no-defaults", "--initialize-insecure", "--basedir=/Users/guanghao/private/graduate-job-match/.local/mysql", "--datadir=/Users/guanghao/private/graduate-job-match/.local/mysql-data-retry", "--default-time-zone=+00:00"]` | 0 | [mysql-initialize-retry.log](mysql-initialize-retry.log) |
| 2026-10-27T01:50:54.270829+00:00 | backend | `["env", "JAVA_HOME=/Users/guanghao/private/graduate-job-match/.local/jdk", "../.local/maven/bin/mvn", "-B", "-Dmaven.repo.local=/Users/guanghao/private/graduate-job-match/.local/m2", "test"]` | 0 | [backend-tests-retry.log](backend-tests-retry.log) |
| 2026-10-27T01:52:17.981470+00:00 | . | `["python3", "scripts/m0/local.py", "db-start"]` | 0 | [mysql-start.log](mysql-start.log) |
| 2026-10-27T01:52:19.096179+00:00 | . | `["python3", "scripts/m0/local.py", "db-setup"]` | 0 | [mysql-setup.log](mysql-setup.log) |
| 2026-10-27T01:52:52.645691+00:00 | . | `["python3", "scripts/m0/seed.py"]` | 0 | [demo-seed.log](demo-seed.log) |
| 2026-10-27T01:52:53.023964+00:00 | . | `["python3", "scripts/m0/local.py", "frontend"]` | 0 | [frontend-start.log](frontend-start.log) |
| 2026-10-27T01:52:49.759283+00:00 | backend | `["env", "JAVA_HOME=/Users/guanghao/private/graduate-job-match/.local/jdk", "../.local/maven/bin/mvn", "-B", "-Dmaven.repo.local=/Users/guanghao/private/graduate-job-match/.local/m2", "package"]` | 0 | [backend-package.log](backend-package.log) |
| 2026-10-27T01:54:01.849776+00:00 | . | `["python3", "scripts/m0/local.py", "backend"]` | 0 | [backend-start.log](backend-start.log) |
| 2026-10-27T01:54:19.555036+00:00 | . | `["python3", "scripts/m0/smoke.py"]` | 0 | [api-smoke.log](api-smoke.log) |
| 2026-10-27T01:57:50.769511+00:00 | . | `["python3", "scripts/m0/probes.py"]` | 0 | [supplemental-probes.log](supplemental-probes.log) |
| 2026-10-27T01:58:32.133356+00:00 | . | `["python3", "scripts/m0/restart_check.py"]` | 0 | [restart-check.log](restart-check.log) |
| 2026-10-27T01:59:07.922034+00:00 | . | `["python3", "scripts/m0/capture.py"]` | 0 | [capture.log](capture.log) |
| 2026-10-27T01:59:09.085019+00:00 | . | `["python3", "scripts/m0/install_tools.py"]` | 0 | [install-tools-cached.log](install-tools-cached.log) |
| 2026-10-27T02:06:05.165173+00:00 | . | `["sysctl", "-n", "machdep.cpu.brand_string", "hw.memsize"]` | 0 | [hardware.log](hardware.log) |
| 2026-10-27T02:06:05.193306+00:00 | . | `["python3", "scripts/m0/local.py", "status"]` | 0 | [runtime-status.log](runtime-status.log) |
| 2026-10-27T02:06:05.288746+00:00 | . | `["gh", "api", "repos/jackyalana/graduate-job-match", "--jq", "{full_name:.full_name,private:.private,visibility:.visibility,html_url:.html_url}"]` | 0 | [repository-privacy.log](repository-privacy.log) |
| 2026-10-27T02:06:40.495531+00:00 | . | `["python3", "scripts/m0/install_tools.py", "--validate-only"]` | 0 | [validate-tool-archives.log](validate-tool-archives.log) |
| 2026-10-27T02:07:56.886051+00:00 | . | `["python3", "scripts/m0/verify_delivery.py"]` | 0 | [final-verification.log](final-verification.log) |

包装器启用前的失败和源码阅读见bootstrap-observations.md。浏览器不产生shell退出码，见browser-observations.md。未执行阶段不列为通过；早期失败均保留。Git提交与推送在本表封存后执行，最终SHA、远程对齐和工作区状态见交接消息；不在本文件制造自引用SHA。
