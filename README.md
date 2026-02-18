# lan-recruitment（毕业设计-智能招聘系统）

## 项目结构

本仓库为 Monorepo 结构：

- `/backend`：后端（Java 8 + Spring Boot + MyBatis-Plus + Sa-Token + MySQL）
- `/frontend`：前端（Vue 3 + Vite + Element Plus + Axios）
- `/scripts`：一键启动脚本
- `/docs`：需求与规范文档

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

```bash
bash scripts/dev.sh
```

## 内置账号

> 执行 backend/sql/seed_demo_data.sql 生成内置帐号

- 管理员：admin / 123456
- 求职者：user / 123456
- HR：hr / 123456
