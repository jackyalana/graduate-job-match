-- Run after duplicate (user_id, job_id) audit and full backup; no history is deleted.
ALTER TABLE job_apply ADD COLUMN version BIGINT NOT NULL DEFAULT 1,
 ADD COLUMN profile_snapshot JSON NULL, ADD COLUMN resume_snapshot JSON NULL,
 ADD COLUMN job_snapshot JSON NULL, ADD COLUMN match_snapshot JSON NULL,
 ADD COLUMN attachment_snapshot VARCHAR(255) NULL,
 ADD UNIQUE KEY uk_apply_user_job (user_id,job_id);
CREATE TABLE application_event (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, application_id BIGINT NOT NULL,
 from_status INT NULL, to_status INT NOT NULL, actor_id BIGINT NOT NULL,
 reason VARCHAR(255) NULL, created_at DATETIME NOT NULL,
 KEY idx_application_event (application_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_job_action (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, job_id BIGINT NOT NULL,
 action_type VARCHAR(16) NOT NULL, active TINYINT NOT NULL, updated_at DATETIME NOT NULL,
 UNIQUE KEY uk_user_job_action(user_id,job_id,action_type), KEY idx_action_job(job_id,action_type,active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE behavior_event (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, job_id BIGINT NOT NULL,
 event_type VARCHAR(24) NOT NULL, event_time DATETIME NOT NULL, dedupe_key VARCHAR(64) NOT NULL,
 UNIQUE KEY uk_behavior_dedupe(dedupe_key), KEY idx_behavior_user_time(user_id,event_time), KEY idx_behavior_job_time(job_id,event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
