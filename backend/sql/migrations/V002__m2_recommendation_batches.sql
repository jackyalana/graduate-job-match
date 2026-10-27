-- Apply to the explicitly selected isolated project database after V001 and backup.
CREATE TABLE recommendation_batch (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  resume_id BIGINT NULL,
  created_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  algorithm_version VARCHAR(64) NOT NULL,
  input_hash CHAR(64) NOT NULL,
  total_eligible INT NOT NULL,
  filters_json TEXT NOT NULL,
  result_json LONGTEXT NOT NULL,
  INDEX idx_batch_owner (user_id, id),
  INDEX idx_batch_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Optional canonical skill mapping; an alias can map only to one canonical skill.
CREATE TABLE skill_tag_alias (
  alias_tag_id BIGINT NOT NULL PRIMARY KEY,
  canonical_tag_id BIGINT NOT NULL,
  KEY idx_skill_alias_canonical (canonical_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
