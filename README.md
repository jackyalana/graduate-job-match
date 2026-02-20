# lan-recruitment（毕业设计-智能招聘系统）

![功能演示](docs/img/功能演示.png)

## 项目结构

本仓库为 Monorepo 结构：

- `/backend`：后端（Java 8 + Spring Boot + MyBatis-Plus + Sa-Token + MySQL）
- `/frontend`：前端（Vue 3 + Vite + Element Plus + Axios）
- `/scripts`：一键启动脚本
- `/docs`：需求与规范文档

## 推荐算法实现

系统内的“岗位推荐 / 简历筛选排序”基于简历标签与岗位标签计算匹配度，并按匹配度从高到低排序展示。

- 向量定义：将简历与岗位都表示为稀疏向量 `{tagId -> weight}`（数据来自 `resume_tag` / `job_tag`）
- 权重策略：标签配置包含 `baseWeight`；简历技能标签（SKILL）会结合熟练度 `proficiency` 放大权重（`baseWeight * proficiency`），非技能类按 `baseWeight`
- 匹配度计算：使用余弦相似度 `score = dot(a,b) / (|a|*|b|)`，结果保留 4 位小数（核心实现见 `backend/src/main/java/com/lanrecruitment/service/impl/MatchServiceImpl.java`）
- 推荐流程：按筛选条件生成候选岗位集合（默认取前 200 个候选），逐个计算匹配分并落库（`match_score`），再按 `matchScore` 倒序返回
- HR 侧排序：HR 查看岗位投递列表时，也会为每份投递简历计算匹配分并按分数倒序排序
- 兜底策略：当用户无可用简历时，推荐接口退化为对候选岗位随机排序返回（不计算匹配分）

### 匹配分缓存（memory / redis 可切换）

为降低重复计算与提升接口可用性，系统对 `(resumeId, jobId)` 的匹配分进行缓存：`computeAndSave` 会优先命中缓存，未命中才会读取 `resume_tag/job_tag` 并计算余弦相似度。

- 缓存开关：`lan.match.cache.store=memory|redis`（默认 memory）
- 过期时间：静态常量（见 `backend/src/main/java/com/lanrecruitment/cache/MatchScoreCacheConstants.java`）
- key 前缀：`lan:recruitment:match:`（Redis 会额外维护 resume/job 反向索引用于失效）
- 失效策略：简历标签或岗位标签变化时，会清理对应缓存并删除相关 `match_score` 记录，确保下一次请求重新匹配

## 后端启动

1. 执行数据库初始化 SQL ：`/backend/sql/init.sql`

2. 配置 `/backend/src/main/resources/application-dev.yml` ：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lan_recruitment?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456 # 修改为你的密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  # 如果 lan.sa-token.store=redis，需要配置 Redis 连接
  redis:
    host: localhost
    port: 6379
    password: 123456
    database: 0

lan:
  # Sa-Token 登录态存储方式：memory（默认）/ redis
  sa-token:
    store: redis
  # 匹配分缓存：memory（默认）/ redis
  match:
    cache:
      store: redis
  # 配置邮箱服务
  email:
    host: # 你的 SMTP 服务器地址
    from: # 你的发件人邮箱地址
    password: # 你的邮箱服务商 SMTP 授权码
    username: # 你的 SMTP 服务器认证账号
```

3. 执行启动命令：

```bash
cd backend

mvn -q -DskipTests spring-boot:run
```

4. 健康检查：GET http://localhost:8080/api/health

## 前端启动

1. 配置后端请求地址 `/frontend/.env` ：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

2. 执行启动命令：

```bash
cd frontend

npm i

npm run dev
```

## 一键启动（推荐）

配置好前后端环境后，在项目根目录执行：

- macOS / Linux：

```bash
bash scripts/dev.sh
```

- Windows（PowerShell）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

- Windows（cmd）：

```bat
scripts\dev.cmd
```

## 内置账号

> 执行 backend/sql/seed_demo_data.sql 生成内置帐号

- 管理员：admin / 123456
- 求职者：user / 123456
- HR：hr / 123456
