-- Apply only with an explicitly selected isolated database; no CREATE DATABASE/USE/DROP.
-- MySQL DDL commits implicitly; back up before running and do not rerun after partial failure.
ALTER TABLE sys_user ADD COLUMN email_verified TINYINT NOT NULL DEFAULT 0;
CREATE TABLE student_profile (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, major_code VARCHAR(64) NULL, education_rank TINYINT NULL,
 graduation_year SMALLINT NULL, expected_category_id BIGINT NULL, salary_min INT NULL,
 profile_version BIGINT NOT NULL DEFAULT 1,
 UNIQUE KEY uk_student_profile_user (user_id),
 CONSTRAINT fk_student_profile_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE student_city_preference (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 city_code VARCHAR(64) NOT NULL,
 UNIQUE KEY uk_student_city (user_id,city_code),
 CONSTRAINT fk_student_city_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE job_category (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
 UNIQUE KEY uk_job_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE resume ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE job ADD COLUMN category_id BIGINT NULL, ADD COLUMN city_code VARCHAR(64) NULL,
 ADD COLUMN salary_min INT NULL, ADD COLUMN salary_max INT NULL, ADD COLUMN salary_negotiable TINYINT NULL,
 ADD COLUMN education_min_rank TINYINT NULL, ADD COLUMN graduate_year_min SMALLINT NULL,
 ADD COLUMN graduate_year_max SMALLINT NULL, ADD COLUMN accept_fresh_graduate TINYINT NULL,
 ADD COLUMN deadline DATETIME NULL, ADD COLUMN version BIGINT NOT NULL DEFAULT 1,
 ADD KEY idx_job_visibility (audit_status,status,deadline), ADD KEY idx_job_city_category (city_code,category_id);
-- Only the lowest-id valid default resume is a migration input. Unknown text remains NULL.
INSERT INTO student_profile(user_id,education_rank,profile_version)
SELECT u.id, CASE TRIM(r.education) WHEN '专科' THEN 1 WHEN '大专' THEN 1 WHEN '本科' THEN 2 WHEN '硕士' THEN 3 WHEN '博士' THEN 4 ELSE NULL END, 1
FROM sys_user u LEFT JOIN resume r ON r.id=(SELECT MIN(r2.id) FROM resume r2 WHERE r2.user_id=u.id AND r2.status=1 AND r2.is_default=1)
WHERE u.role='USER';
