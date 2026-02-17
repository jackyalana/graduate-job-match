SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS lan_recruitment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lan_recruitment;

START TRANSACTION;

SET @pwd_123456 := '$2y$10$RCmaUNsyLi.ANO9f5QY5fuGYuE0zq1pCd4TcnaI2zKwczW.hMVcMa';

INSERT INTO sys_user (username, password, email, role, company_name, avatar, status, audit_status, real_name, phone, school, graduate_date)
VALUES ('admin', @pwd_123456, 'admin@lanrecruitment.local', 'ADMIN', NULL, NULL, 1, 1, NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  company_name = VALUES(company_name),
  avatar = VALUES(avatar),
  status = VALUES(status),
  audit_status = VALUES(audit_status),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  school = VALUES(school),
  graduate_date = VALUES(graduate_date);

INSERT INTO sys_user (username, password, email, role, company_name, avatar, status, audit_status, real_name, phone, school, graduate_date)
VALUES ('hr', @pwd_123456, 'hr@lanrecruitment.local', 'HR', '字节跳动', 'https://i.pravatar.cc/150?img=12', 1, 1, '张晨', '13800000001', '北京邮电大学', '2020-06-30')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  company_name = VALUES(company_name),
  avatar = VALUES(avatar),
  status = VALUES(status),
  audit_status = VALUES(audit_status),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  school = VALUES(school),
  graduate_date = VALUES(graduate_date);

INSERT INTO sys_user (username, password, email, role, company_name, avatar, status, audit_status, real_name, phone, school, graduate_date)
VALUES ('user', @pwd_123456, 'user@lanrecruitment.local', 'USER', NULL, 'https://i.pravatar.cc/150?img=5', 1, 1, '李沐', '13800000002', '同济大学', '2026-06-30')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  company_name = VALUES(company_name),
  avatar = VALUES(avatar),
  status = VALUES(status),
  audit_status = VALUES(audit_status),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  school = VALUES(school),
  graduate_date = VALUES(graduate_date);

SET @hr_id := (SELECT id FROM sys_user WHERE username = 'hr' LIMIT 1);
SET @user_id := (SELECT id FROM sys_user WHERE username = 'user' LIMIT 1);

INSERT INTO tag (tag_name, tag_type, base_weight, status) VALUES
('Java', 'SKILL', 5, 1),
('Spring Boot', 'SKILL', 5, 1),
('MyBatis-Plus', 'SKILL', 4, 1),
('MySQL', 'SKILL', 4, 1),
('Redis', 'SKILL', 4, 1),
('Kafka', 'SKILL', 3, 1),
('Docker', 'SKILL', 3, 1),
('Kubernetes', 'SKILL', 3, 1),
('微服务', 'SKILL', 4, 1),
('分布式系统', 'SKILL', 4, 1),
('Linux', 'SKILL', 3, 1),
('计算机网络', 'SKILL', 3, 1),
('操作系统', 'SKILL', 3, 1),
('数据结构与算法', 'SKILL', 3, 1),

('JavaScript', 'SKILL', 5, 1),
('TypeScript', 'SKILL', 5, 1),
('Vue', 'SKILL', 5, 1),
('Vue 3', 'SKILL', 3, 1),
('React', 'SKILL', 4, 1),
('HTML', 'SKILL', 4, 1),
('CSS', 'SKILL', 4, 1),
('Vite', 'SKILL', 3, 1),
('Webpack', 'SKILL', 3, 1),
('Node.js', 'SKILL', 3, 1),
('性能优化', 'SKILL', 3, 1),
('可视化', 'SKILL', 2, 1),

('Hadoop', 'SKILL', 4, 1),
('Hive', 'SKILL', 4, 1),
('Spark', 'SKILL', 4, 1),
('Flink', 'SKILL', 5, 1),
('HBase', 'SKILL', 3, 1),
('ClickHouse', 'SKILL', 4, 1),
('数据仓库', 'SKILL', 4, 1),
('实时计算', 'SKILL', 4, 1),
('离线计算', 'SKILL', 3, 1),
('指标体系', 'SKILL', 3, 1),

('Python', 'SKILL', 5, 1),
('PyTorch', 'SKILL', 5, 1),
('TensorFlow', 'SKILL', 4, 1),
('机器学习', 'SKILL', 5, 1),
('深度学习', 'SKILL', 5, 1),
('推荐系统', 'SKILL', 5, 1),
('自然语言处理', 'SKILL', 4, 1),
('计算机视觉', 'SKILL', 4, 1),
('特征工程', 'SKILL', 4, 1),
('MLOps', 'SKILL', 3, 1),

('电商', 'INDUSTRY', 2, 1),
('内容平台', 'INDUSTRY', 2, 1),
('直播', 'INDUSTRY', 2, 1),
('搜索', 'INDUSTRY', 2, 1),
('广告', 'INDUSTRY', 2, 1),
('前端开发', 'INDUSTRY', 1, 1),
('后端开发', 'INDUSTRY', 1, 1),

('校招', 'EXPERIENCE', 2, 1),
('社招', 'EXPERIENCE', 2, 1),
('实习', 'EXPERIENCE', 2, 1),
('0-1年', 'EXPERIENCE', 2, 1),
('1-3年', 'EXPERIENCE', 2, 1),
('3年经验', 'EXPERIENCE', 1, 1),
('3-5年', 'EXPERIENCE', 2, 1),
('5年以上', 'EXPERIENCE', 2, 1),
('应届生', 'EXPERIENCE', 1, 1)
ON DUPLICATE KEY UPDATE
  base_weight = VALUES(base_weight),
  status = VALUES(status);

SET @t_java := (SELECT id FROM tag WHERE tag_name='Java' AND tag_type='SKILL' LIMIT 1);
SET @t_spring := (SELECT id FROM tag WHERE tag_name='Spring Boot' AND tag_type='SKILL' LIMIT 1);
SET @t_mybatis := (SELECT id FROM tag WHERE tag_name='MyBatis-Plus' AND tag_type='SKILL' LIMIT 1);
SET @t_mysql := (SELECT id FROM tag WHERE tag_name='MySQL' AND tag_type='SKILL' LIMIT 1);
SET @t_redis := (SELECT id FROM tag WHERE tag_name='Redis' AND tag_type='SKILL' LIMIT 1);
SET @t_kafka := (SELECT id FROM tag WHERE tag_name='Kafka' AND tag_type='SKILL' LIMIT 1);
SET @t_docker := (SELECT id FROM tag WHERE tag_name='Docker' AND tag_type='SKILL' LIMIT 1);
SET @t_k8s := (SELECT id FROM tag WHERE tag_name='Kubernetes' AND tag_type='SKILL' LIMIT 1);
SET @t_micro := (SELECT id FROM tag WHERE tag_name='微服务' AND tag_type='SKILL' LIMIT 1);
SET @t_dist := (SELECT id FROM tag WHERE tag_name='分布式系统' AND tag_type='SKILL' LIMIT 1);
SET @t_linux := (SELECT id FROM tag WHERE tag_name='Linux' AND tag_type='SKILL' LIMIT 1);
SET @t_net := (SELECT id FROM tag WHERE tag_name='计算机网络' AND tag_type='SKILL' LIMIT 1);
SET @t_os := (SELECT id FROM tag WHERE tag_name='操作系统' AND tag_type='SKILL' LIMIT 1);
SET @t_algo := (SELECT id FROM tag WHERE tag_name='数据结构与算法' AND tag_type='SKILL' LIMIT 1);

SET @t_js := (SELECT id FROM tag WHERE tag_name='JavaScript' AND tag_type='SKILL' LIMIT 1);
SET @t_ts := (SELECT id FROM tag WHERE tag_name='TypeScript' AND tag_type='SKILL' LIMIT 1);
SET @t_vue := (SELECT id FROM tag WHERE tag_name='Vue' AND tag_type='SKILL' LIMIT 1);
SET @t_react := (SELECT id FROM tag WHERE tag_name='React' AND tag_type='SKILL' LIMIT 1);
SET @t_html := (SELECT id FROM tag WHERE tag_name='HTML' AND tag_type='SKILL' LIMIT 1);
SET @t_css := (SELECT id FROM tag WHERE tag_name='CSS' AND tag_type='SKILL' LIMIT 1);
SET @t_vite := (SELECT id FROM tag WHERE tag_name='Vite' AND tag_type='SKILL' LIMIT 1);
SET @t_webpack := (SELECT id FROM tag WHERE tag_name='Webpack' AND tag_type='SKILL' LIMIT 1);
SET @t_node := (SELECT id FROM tag WHERE tag_name='Node.js' AND tag_type='SKILL' LIMIT 1);
SET @t_perf := (SELECT id FROM tag WHERE tag_name='性能优化' AND tag_type='SKILL' LIMIT 1);
SET @t_vis := (SELECT id FROM tag WHERE tag_name='可视化' AND tag_type='SKILL' LIMIT 1);

SET @t_hadoop := (SELECT id FROM tag WHERE tag_name='Hadoop' AND tag_type='SKILL' LIMIT 1);
SET @t_hive := (SELECT id FROM tag WHERE tag_name='Hive' AND tag_type='SKILL' LIMIT 1);
SET @t_spark := (SELECT id FROM tag WHERE tag_name='Spark' AND tag_type='SKILL' LIMIT 1);
SET @t_flink := (SELECT id FROM tag WHERE tag_name='Flink' AND tag_type='SKILL' LIMIT 1);
SET @t_hbase := (SELECT id FROM tag WHERE tag_name='HBase' AND tag_type='SKILL' LIMIT 1);
SET @t_ck := (SELECT id FROM tag WHERE tag_name='ClickHouse' AND tag_type='SKILL' LIMIT 1);
SET @t_dwh := (SELECT id FROM tag WHERE tag_name='数据仓库' AND tag_type='SKILL' LIMIT 1);
SET @t_stream := (SELECT id FROM tag WHERE tag_name='实时计算' AND tag_type='SKILL' LIMIT 1);
SET @t_batch := (SELECT id FROM tag WHERE tag_name='离线计算' AND tag_type='SKILL' LIMIT 1);
SET @t_metric := (SELECT id FROM tag WHERE tag_name='指标体系' AND tag_type='SKILL' LIMIT 1);

SET @t_py := (SELECT id FROM tag WHERE tag_name='Python' AND tag_type='SKILL' LIMIT 1);
SET @t_torch := (SELECT id FROM tag WHERE tag_name='PyTorch' AND tag_type='SKILL' LIMIT 1);
SET @t_tf := (SELECT id FROM tag WHERE tag_name='TensorFlow' AND tag_type='SKILL' LIMIT 1);
SET @t_ml := (SELECT id FROM tag WHERE tag_name='机器学习' AND tag_type='SKILL' LIMIT 1);
SET @t_dl := (SELECT id FROM tag WHERE tag_name='深度学习' AND tag_type='SKILL' LIMIT 1);
SET @t_rec := (SELECT id FROM tag WHERE tag_name='推荐系统' AND tag_type='SKILL' LIMIT 1);
SET @t_nlp := (SELECT id FROM tag WHERE tag_name='自然语言处理' AND tag_type='SKILL' LIMIT 1);
SET @t_cv := (SELECT id FROM tag WHERE tag_name='计算机视觉' AND tag_type='SKILL' LIMIT 1);
SET @t_fe := (SELECT id FROM tag WHERE tag_name='特征工程' AND tag_type='SKILL' LIMIT 1);
SET @t_mlops := (SELECT id FROM tag WHERE tag_name='MLOps' AND tag_type='SKILL' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-前端开发工程师（中台平台）', '字节跳动', '北京', '25K-45K·14薪', '前端开发',
  '岗位职责：\n1. 负责字节跳动中台/平台类产品的前端研发，面向多业务线提供高复用能力的组件与解决方案。\n2. 参与业务架构设计与技术方案落地，包括工程化、性能优化、可维护性提升。\n3. 与产品/设计/后端紧密协作，推动需求高质量交付。\n\n岗位要求：\n1. 熟悉 JavaScript/TypeScript，掌握至少一种主流框架（Vue/React）。\n2. 熟悉前端工程化（Vite/Webpack）、组件化开发与常见性能优化手段。\n3. 有良好的编码习惯与抽象能力，能推动复杂需求拆解与落地。\n\n加分项：\n1. 有大型中后台系统研发经验。\n2. 有可视化、低代码、微前端等相关经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-前端开发工程师（中台平台）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-前端开发实习生（Web基础平台）', '字节跳动', '上海', '200-350元/天', '前端开发',
  '岗位职责：\n1. 参与基础平台前端能力建设，完善组件库与基础工具链。\n2. 参与页面开发与联调，保证质量与交付效率。\n3. 参与性能与体验优化（首屏、交互流畅度、稳定性）。\n\n岗位要求：\n1. 熟悉 HTML/CSS/JavaScript，了解 TypeScript。\n2. 了解 Vue 或 React，有项目实践经验。\n3. 有良好的学习能力与沟通协作能力。\n\n实习信息：\n1. 可实习 3 个月以上优先。\n2. 有转正机会。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-前端开发实习生（Web基础平台）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-后端开发工程师（Java/交易平台）', '字节跳动', '北京', '28K-55K·14薪', '后端开发',
  '岗位职责：\n1. 负责交易平台核心服务的设计、研发与持续迭代，保障高并发、高可用与数据一致性。\n2. 参与微服务架构治理（限流、熔断、降级、可观测性）与性能优化。\n3. 与上下游团队协作，推动系统稳定性与研发效率提升。\n\n岗位要求：\n1. 熟悉 Java 与 Spring Boot，熟悉 MySQL/Redis 等基础组件。\n2. 理解常见分布式问题（CAP、一致性、幂等、分布式锁）。\n3. 具备良好的工程素养与问题定位能力。\n\n加分项：\n1. 有 Kafka/消息系统使用经验。\n2. 有大型系统容量规划与压测经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-后端开发工程师（Java/交易平台）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-后端开发实习生（Java/微服务）', '字节跳动', '深圳', '220-380元/天', '后端开发',
  '岗位职责：\n1. 参与业务服务开发、接口设计与联调。\n2. 参与基础能力建设（配置中心、日志、监控、告警）与线上问题排查。\n3. 参与单元测试与质量保障。\n\n岗位要求：\n1. 熟悉 Java 基础与面向对象。\n2. 了解 Spring Boot 与常用中间件（MySQL/Redis）。\n3. 具备良好的编码规范与学习能力。\n\n实习信息：\n1. 可实习 3-6 个月优先。\n2. 有转正机会。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-后端开发实习生（Java/微服务）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-大数据开发工程师（Flink 实时计算）', '字节跳动', '杭州', '26K-50K·14薪', '大数据',
  '岗位职责：\n1. 负责实时数据链路建设与优化，基于 Flink 构建稳定的实时计算任务。\n2. 参与数据质量、指标体系与数据服务建设，提升数据可用性。\n3. 推动任务治理与成本优化（延迟、吞吐、资源、稳定性）。\n\n岗位要求：\n1. 熟悉 Flink/Spark/Hive 至少一种计算引擎，有生产实践经验。\n2. 熟悉数据建模、指标体系与数据仓库基本方法。\n3. 具备扎实的编程能力（Java/Scala/Python 之一）。\n\n加分项：\n1. 有 Kafka/ClickHouse 等组件经验。\n2. 有实时任务运维与故障恢复经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-大数据开发工程师（Flink 实时计算）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-大数据平台研发实习生（Spark/Hive）', '字节跳动', '成都', '200-350元/天', '大数据',
  '岗位职责：\n1. 参与离线/实时数据任务开发与维护，支持业务分析与指标需求。\n2. 参与数据质量建设与任务治理。\n3. 参与平台能力优化（调度、元数据、监控）。\n\n岗位要求：\n1. 了解 Hive/Spark/Hadoop 等组件。\n2. 具备一定的 SQL 能力与数据处理思维。\n3. 熟悉 Java/Scala/Python 任一语言。\n\n实习信息：\n1. 可实习 3 个月以上优先。\n2. 有转正机会。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-大数据平台研发实习生（Spark/Hive）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-算法工程师（推荐系统）', '字节跳动', '北京', '35K-75K·16薪', '算法',
  '岗位职责：\n1. 负责推荐系统核心算法研发与迭代（召回/粗排/精排/重排），提升点击、时长与用户体验。\n2. 参与特征工程与模型训练优化，建设高效稳定的训练与推理链路。\n3. 结合业务场景进行实验设计与效果评估，推动算法落地。\n\n岗位要求：\n1. 熟悉机器学习/深度学习基础，熟悉常见推荐算法与评估方法。\n2. 熟悉 Python，熟练使用 PyTorch/TensorFlow 之一。\n3. 具备良好的工程能力与数据分析能力。\n\n加分项：\n1. 有大规模推荐/广告/搜索实践经验。\n2. 有在线学习、因果推断、A/B 实验平台经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-算法工程师（推荐系统）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-算法实习生（NLP）', '字节跳动', '上海', '300-500元/天', '算法',
  '岗位职责：\n1. 参与 NLP 方向算法研发与实验，支持文本理解、生成、匹配等任务。\n2. 参与数据清洗、标注策略设计与评估。\n3. 参与模型训练与推理优化。\n\n岗位要求：\n1. 熟悉机器学习基础与深度学习框架（PyTorch/TensorFlow）。\n2. 熟悉 Python，具备良好的编码与实验习惯。\n3. 具备较强的学习能力与科研阅读能力。\n\n实习信息：\n1. 可实习 3 个月以上优先。\n2. 有转正机会。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-算法实习生（NLP）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-机器学习平台工程师（MLOps）', '字节跳动', '深圳', '28K-60K·14薪', '算法',
  '岗位职责：\n1. 建设与优化机器学习平台能力（训练、评估、发布、监控、回滚），提升研发效率。\n2. 参与模型生命周期治理与线上稳定性保障。\n3. 与算法团队合作推进工程化落地。\n\n岗位要求：\n1. 熟悉 Python/Java/Go 之一，具备良好工程能力。\n2. 了解机器学习训练流程与常见框架。\n3. 熟悉容器化与集群调度（Docker/K8s）优先。\n\n加分项：\n1. 有特征平台、在线推理服务、模型监控经验。\n2. 有大规模分布式训练经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-机器学习平台工程师（MLOps）' LIMIT 1);

INSERT INTO job (hr_id, job_name, company_name, city, salary_range, job_type, description, status, audit_status, created_at)
SELECT @hr_id, '字节跳动-数据研发工程师（ClickHouse/指标体系）', '字节跳动', '杭州', '24K-45K·14薪', '数据研发',
  '岗位职责：\n1. 负责指标体系建设与数据服务设计，面向业务提供可靠一致的数据口径。\n2. 基于 ClickHouse 等 OLAP 引擎优化查询性能与成本。\n3. 参与数据建模、数据资产治理与质量体系建设。\n\n岗位要求：\n1. 熟悉 SQL 与数据建模方法，具备数据仓库/指标体系实践。\n2. 熟悉 ClickHouse/Hive/Spark 等至少一种数据组件。\n3. 具备良好的问题分析与跨团队协作能力。\n\n加分项：\n1. 有数据治理、血缘、元数据建设经验。\n2. 有实时/离线融合架构经验。',
  1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-数据研发工程师（ClickHouse/指标体系）' LIMIT 1);

SET @j_fe := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-前端开发工程师（中台平台）' ORDER BY id DESC LIMIT 1);
SET @j_fe_intern := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-前端开发实习生（Web基础平台）' ORDER BY id DESC LIMIT 1);
SET @j_be := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-后端开发工程师（Java/交易平台）' ORDER BY id DESC LIMIT 1);
SET @j_be_intern := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-后端开发实习生（Java/微服务）' ORDER BY id DESC LIMIT 1);
SET @j_bd := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-大数据开发工程师（Flink 实时计算）' ORDER BY id DESC LIMIT 1);
SET @j_bd_intern := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-大数据平台研发实习生（Spark/Hive）' ORDER BY id DESC LIMIT 1);
SET @j_algo := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-算法工程师（推荐系统）' ORDER BY id DESC LIMIT 1);
SET @j_algo_intern := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-算法实习生（NLP）' ORDER BY id DESC LIMIT 1);
SET @j_mlops := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-机器学习平台工程师（MLOps）' ORDER BY id DESC LIMIT 1);
SET @j_data := (SELECT id FROM job WHERE hr_id=@hr_id AND job_name='字节跳动-数据研发工程师（ClickHouse/指标体系）' ORDER BY id DESC LIMIT 1);

INSERT IGNORE INTO job_tag (job_id, tag_id, proficiency, weight) VALUES
(@j_fe, @t_js, 3, 5),
(@j_fe, @t_ts, 2, 4),
(@j_fe, @t_vue, 2, 4),
(@j_fe, @t_vite, 2, 2),
(@j_fe, @t_webpack, 2, 2),
(@j_fe, @t_perf, 2, 2),
(@j_fe, @t_vis, 1, 1),

(@j_fe_intern, @t_html, 2, 3),
(@j_fe_intern, @t_css, 2, 3),
(@j_fe_intern, @t_js, 2, 4),
(@j_fe_intern, @t_vue, 1, 2),
(@j_fe_intern, @t_ts, 1, 2),

(@j_be, @t_java, 3, 5),
(@j_be, @t_spring, 3, 5),
(@j_be, @t_mysql, 2, 4),
(@j_be, @t_redis, 2, 4),
(@j_be, @t_kafka, 1, 2),
(@j_be, @t_micro, 2, 3),
(@j_be, @t_dist, 2, 3),

(@j_be_intern, @t_java, 2, 4),
(@j_be_intern, @t_spring, 1, 2),
(@j_be_intern, @t_mysql, 1, 2),
(@j_be_intern, @t_redis, 1, 2),

(@j_bd, @t_flink, 3, 5),
(@j_bd, @t_kafka, 2, 3),
(@j_bd, @t_dwh, 2, 3),
(@j_bd, @t_stream, 2, 4),
(@j_bd, @t_spark, 1, 2),
(@j_bd, @t_hive, 1, 2),

(@j_bd_intern, @t_hive, 2, 3),
(@j_bd_intern, @t_spark, 2, 3),
(@j_bd_intern, @t_hadoop, 1, 2),
(@j_bd_intern, @t_batch, 1, 2),

(@j_algo, @t_py, 3, 5),
(@j_algo, @t_torch, 2, 4),
(@j_algo, @t_ml, 3, 5),
(@j_algo, @t_dl, 2, 4),
(@j_algo, @t_rec, 3, 5),
(@j_algo, @t_fe, 2, 3),

(@j_algo_intern, @t_py, 2, 4),
(@j_algo_intern, @t_nlp, 2, 4),
(@j_algo_intern, @t_torch, 1, 2),

(@j_mlops, @t_mlops, 2, 4),
(@j_mlops, @t_docker, 2, 3),
(@j_mlops, @t_k8s, 2, 3),
(@j_mlops, @t_py, 2, 3),
(@j_mlops, @t_linux, 2, 2),

(@j_data, @t_ck, 2, 4),
(@j_data, @t_metric, 2, 3),
(@j_data, @t_dwh, 2, 3),
(@j_data, @t_hive, 1, 2),
(@j_data, @t_spark, 1, 2);

INSERT INTO resume (user_id, resume_name, real_name, gender, city, education, work_years, expect_job_type, expect_salary, work_desc, is_default, status)
SELECT @user_id, '我的技术简历', '李沐', 'MALE', '上海', '本科', '应届', '前端开发', '20K-30K', '项目经历：\n1. 基于 Vue3 + TypeScript 实现管理后台，包含权限路由、表格大数据量渲染与性能优化。\n2. 参与组件库沉淀与工程化建设（Vite/ESLint/Prettier）。\n\n能力亮点：\n1. 熟悉前端基础与工程化。\n2. 对性能与体验优化有实践经验。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM resume WHERE user_id=@user_id AND resume_name='我的技术简历' LIMIT 1);

SET @resume_id := (SELECT id FROM resume WHERE user_id=@user_id AND resume_name='我的技术简历' ORDER BY id DESC LIMIT 1);

INSERT IGNORE INTO resume_tag (resume_id, tag_id, proficiency, weight) VALUES
(@resume_id, @t_js, 3, 5),
(@resume_id, @t_ts, 2, 4),
(@resume_id, @t_vue, 2, 4),
(@resume_id, @t_html, 2, 3),
(@resume_id, @t_css, 2, 3),
(@resume_id, @t_vite, 1, 2),
(@resume_id, @t_perf, 1, 2),
(@resume_id, @t_net, NULL, 1),
(@resume_id, @t_os, NULL, 1),
(@resume_id, @t_algo, NULL, 1);

INSERT INTO notice (title, content, status, created_at)
SELECT
  '【系统更新】智能招聘系统上线啦！',
  '亲爱的用户：\n\n智能招聘系统已正式上线。本次更新重点如下：\n1. 岗位浏览体验升级：支持关键字/城市/岗位类型/标签多维筛选，并提供推荐岗位与最新岗位两种浏览方式。\n2. 智能匹配推荐：系统将结合简历标签与岗位标签计算匹配度，帮助你更高效地发现合适机会。\n3. 投递流程优化：一键投递默认简历，投递记录可追踪。\n4. HR 侧能力完善：岗位发布、标签配置、投递管理流程已打通。\n\n温馨提示：\n- 为获得更精准推荐，请尽快创建简历并完善标签。\n- 如遇到问题，欢迎反馈，我们将持续迭代。\n\n祝大家求职顺利 / 招聘高效！',
  1,
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title='【系统更新】智能招聘系统上线啦！' LIMIT 1);

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
