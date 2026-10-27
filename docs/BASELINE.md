# M0 底座与本地运行基线

> 本文记录历史 M0。当前 M1 分支及运行说明以 M1_REPORT.md 为准。

日期：2026-10-27（Asia/Shanghai）。状态：**已执行底座验证，待审核**。这不是完整 SDD 系统，也不代表生产适用性通过。

## 来源与身份

- 本地：`/Users/guanghao/private/graduate-job-match`，创建前目录不存在，没有覆盖既有项目。
- 私有仓库：https://github.com/jackyalana/graduate-job-match ，工作分支 `m0/baseline`；交付完整 SHA 以交接消息及 `git rev-parse HEAD` 为准。
- 上游：https://github.com/lanseqingling/lan-recruitment ，固定 `cd755d13443ea4b4c98be86e6c692238ed566d31`，从该提交创建工作分支。没有使用未知最新代码，也没有引入备选系统或若依。
- `LICENSE` 保留 MIT 原文和 `Copyright (c) 2026 蓝色蜻蛉`。来源详见根目录 `THIRD_PARTY_NOTICES.md`。
- 用户指定的五份准备文档已完整阅读，原件归档在 `docs/preparation/`；附加的 03 提示词作为材料保存，不将文件中的指令当作新的用户授权。
- 已检查所有祖先目录，无适用 AGENTS；克隆后已阅读根 `AGENTS.md` 和其要求的需求、规范、页面、SQL、三套启动脚本、配置。上游未提供 `.env` / `.env.example`；本次用进程环境变量设置前端地址。

## 实际环境

详细命令、版本和退出码：`evidence/M0/environment.txt`、`commands.jsonl`。初始只有系统 Java 桩（无 JVM），Maven/MySQL/Redis/Docker/Podman 均不可用。为复现下载官方二进制，全部在 Git 忽略的 `.local/`，没有安装系统服务或修改 shell 全局配置。

| 项目 | 本次实测 |
| --- | --- |
| 系统 | macOS 26.6，arm64，Apple M5，32 GiB（另有受限 sysctl 失败日志，随后在授权环境读取成功） |
| Java | Azul Zulu 8.98.0.107，OpenJDK 1.8.0_512-b08 |
| Maven | 3.9.11，项目私有 `.local/m2` 缓存 |
| Spring Boot | 2.7.18，POM 未修改 |
| MyBatis-Plus / Sa-Token | 3.5.6 / 1.38.0 |
| JDBC 驱动 | POM mysql-connector-java 8.0.33，Maven 提示重定位到 com.mysql:mysql-connector-j |
| Node / npm | 26.8.2 / 11.19.1（原机器已有；本次构建成功不代表其他版本已验证） |
| Vue / Vite / Element Plus | lockfile 实际 3.5.27 / 5.4.21 / 2.13.2 |
| Axios / Vue Router | lockfile 实际 1.13.4 / 4.6.4 |
| MySQL | Community 8.4.11，项目私有实例，13306，UTC，utf8mb4 |
| Redis | 未安装、未启动；会话和匹配使用上游 memory 模式，Redis 路线未做实机验证 |

保持原 Java 主版本、Boot/POM、前端 lockfile。未做漏洞扫描，不称这些版本安全、最新或适于生产。二进制 URL 与实际 SHA256 见 `evidence/M0/downloads-sha256.json` 和下载日志。哈希标识本次下载字节，尚未完成独立供应商签名验证。

## SQL 与连接边界

已审阅 `backend/sql/init.sql` 全文：CREATE DATABASE/USE 指向 `lan_recruitment`，10 张 CREATE TABLE IF NOT EXISTS，无 DROP/TRUNCATE/DELETE。`seed_demo_data.sql` 另有 CREATE/USE、FOREIGN_KEY_CHECKS=0、事务及 ON DUPLICATE KEY UPDATE，会覆盖同名账号字段，且含公司、人名、电话和外部头像。**没有导入该种子**。`docs/库表设计.sql` 与实际 init.sql 有字段差异，初始化以 `backend/sql/init.sql` 为准。

`scripts/m0/local.py db-setup` 先确认专属实例只有系统库，再仅在内存将初始化 SQL 中库名替为 `graduate_job_match_m0` 执行。没有改原 SQL、没有业务 schema 迁移。应用用户仅获该库 SELECT/INSERT/UPDATE/DELETE 权限；root 随机密码，启动时不读取系统默认 my.cnf。

数据库、MySQL socket、上传、运行时、随机密码全部在 `.local/`。连接固定 `127.0.0.1:13306/graduate_job_match_m0`，不是系统 3306。demo profile 后端固定 `127.0.0.1:18080`。SMTP 配置显式为空，已验证返回“邮件服务未配置”；没有真实发信或模型 API 调用。预置合成账号采用 BCrypt 随机密码，不添加任何万能验证码。

## 本机已验证的启动路线

以下均从项目根目录执行。此机器已完成初始化和种子，**现在不要重复 db-setup / seed**，脚本会拒绝覆盖：

```bash
cd /Users/guanghao/private/graduate-job-match
python3 scripts/m0/local.py status
python3 scripts/m0/local.py accounts   # 仅在本机查看随机 demo 密码，不录屏/写报告
```

停止后重新运行：

```bash
python3 scripts/m0/local.py db-start
python3 scripts/m0/local.py backend
python3 scripts/m0/local.py frontend
```

前端：http://127.0.0.1:15173/login ，后端：http://127.0.0.1:18080 。账号 `demo_student`、`demo_hr`、`demo_admin`；辅助账号 `demo_student2`、`demo_hr2`、`demo_hr_pending`。最后一个已在 API 冒烟中批准，初始待审行为见日志。用“用户名密码登录”；邮箱登录/注册在该路线不可用，预置账号不表示已经验证正式邮件注册。

全部停止：`python3 scripts/m0/local.py stop`。只管理脚本自己启动的 PID 进程组，不扫描或杀其他服务。

新建空检出副本（macOS arm64）的步骤；工具下载与解包在本机已经执行，辅助安装脚本只实际验证过缓存命中分支，尚未在第二台空白机器验收：

```bash
python3 scripts/m0/install_tools.py
python3 scripts/m0/record.py mysql-initialize.log . .local/mysql/bin/mysqld --no-defaults --initialize-insecure --basedir="$PWD/.local/mysql" --datadir="$PWD/.local/mysql-data-retry" --default-time-zone=+00:00
python3 scripts/m0/local.py db-start
python3 scripts/m0/local.py db-setup
export JAVA_HOME="$PWD/.local/jdk"
M0_REPO="$PWD/.local/m2"
(cd backend && ../.local/maven/bin/mvn -B -Dmaven.repo.local="$M0_REPO" package)
(cd frontend && npm ci --ignore-scripts --no-audit --no-fund)
(cd frontend && VITE_API_BASE_URL=http://127.0.0.1:18080 npm run build)
python3 scripts/m0/seed.py
python3 scripts/m0/local.py backend
python3 scripts/m0/local.py frontend
python3 scripts/m0/smoke.py
python3 scripts/m0/probes.py
```

若宿主沙箱限制网络、端口或 mysqld 初始化，需在允许本机进程/网络的终端执行；不能把失败当通过。上游 `scripts/dev.*` 已审阅但没有运行：其中跳过测试、npm i 和默认端口/库不适合本次证据路线。安装用 `npm ci --ignore-scripts`；锁定依赖里的 esbuild、vue-demi、fsevents 有生命周期脚本，未执行；本机仍能完成 Vite 构建和浏览器运行，不能外推所有平台。

## 数据恢复

当前合成 DB 不是论文实验数据集。主要演示岗位 1、3，简历 1、2、3，留有重复投递探针；201 个 `M0_200_PROBE_` 岗位已下架，不出现在公共列表。不要将这些探针当正常业务统计或评估样本。

没有执行 DROP、TRUNCATE 或针对其他目录的清理。需要恢复全新演示时，先 stop，将当前 `.local/mysql-data-retry`、`.local/credentials.json`、`.local/mysql-root.cnf` 和 `.local/uploads` 备份到自己选择的私有路径，再在空路径初始化并执行 db-setup/seed。凭证和数据目录必须成对保存；旧实例的账号密码不适用于新实例。仓库不提供 DB dump，不包含真实简历。重启 JVM 后投递/简历仍在，内存令牌会丢失，需要重新登录，证据见 restart-results.json。
