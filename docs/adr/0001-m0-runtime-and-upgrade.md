# ADR-0001：先固定上游复现，升级作为独立变更

日期：2026-10-27。状态：M0 实际采用本地复现路线；后续升级方案待审核。依据：准备包 SDD §3、用户“本轮只完成 M0”。

## 已采用

保留 Java 8、Boot 2.7.18、MyBatis-Plus 3.5.6、Sa-Token 1.38.0 和 Vue 3/Vite 锁文件。私有 JDK 8.0.512 + Maven 3.9.11 + MySQL 8.4.11，均能实测运行。使用显式 demo profile、UTC、memory 会话/缓存、预置合成账号、空 SMTP，端口与文件均隔离。只新增 profile 和 M0 工具，不改推荐算法/业务模型/POM/lockfile。数据库版本是本次实测选择，上游未固定服务端补丁。

理由：保留可识别的原底座，先确认原代码缺陷和 SDD 差距，避免大版本迁移混淆复用审计。代价：保留已识别的附件泄露、重复投递、状态/缓存缺陷，仅在合成本地环境复现；不能直接开放网络或输入真实简历。普通 application.yml 没有被改成验证码绕过模式。

## M0 后必要选项

| 选项 | 适用理由 | 影响与所需验证 |
| --- | --- | --- |
| A：原版本完成受限本地演示 | 当前用户优先路线，构建与核心冒烟已具证据 | M1–M4 仍必须完成 SDD；联网使用前单独完成依赖支持/漏洞评估和修复，不能从本次成功启动推断安全 |
| B：Java 17 + 经选择的兼容 Boot 3 分支 | 学校、运行平台或后续支持策略要求时提案 | javax→jakarta、Sa-Token/MyBatis starter、Validation、Servlet、Spring 测试和 JDBC 回归；具体补丁、支持和漏洞尚未评估，未实施 |
| C：仅修复经确认的依赖风险 | 后续扫描有具体证据时，以小版本独立变更处理 | 不盲目 update 全部；记录旧新锁文件、license、API 差异并重跑受影响测试/浏览器链路 |
| D：Node 版本固定到学校指定环境 | 当前 Node 26 成功只证明本机；交付需要跨机一致性 | 另建变更验证 lockfile 安装和 build，不假定所有 Node 版本兼容 |

安装来源：[Azul 官方 macOS 安装说明](https://docs.azul.com/core/install/macos)、[MySQL 官方 8.4 下载](https://dev.mysql.com/downloads/mysql/8.4.html)、[Maven 中央仓库](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/)。引用只说明二进制来源，不是版本支持/安全背书。

## 与上游仓库规范的冲突处理

上游 AGENTS/编码规范包含“仅 GET/POST”“无外键”“鉴权仅 Controller”“原标签余弦”等要求；用户的 SDD 要求资源方法、对象归属、快照、完整状态、版本和新公式。M0 只记录，不改写业务来掩盖差异。后续以本用户明确提供的 SDD 为实施基线，并在具体迁移 ADR 中解释影响；不把上游文本“规范优先一切”当成覆盖用户需求的授权。

会话当前实际为前端 localStorage 令牌 + satoken header；上游 withCredentials、Sa-Token cookie 读取和宽 CORS 尚未收敛。M1 需固定单一会话方案及撤销策略，普通 common 路径必须检查禁用状态。M0 没有宣称 cookie/CSRF/跨站安全已验收。

## 不属于本次决定

没有切换备选或若依，没有公开部署，没有修改上游，没有 AI/LLM 业务调用，没有完成 Compose 验证。Docker/Podman 缺失，Compose 留到交付阶段另做真实验证，不提供未经运行的模板充数。
