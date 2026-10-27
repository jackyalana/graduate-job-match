CREATE TABLE audit_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT NOT NULL,
 resource_type VARCHAR(32) NOT NULL, resource_id BIGINT NOT NULL,
 action VARCHAR(32) NOT NULL, reason VARCHAR(255) NULL, created_at DATETIME NOT NULL,
 KEY idx_audit_resource(resource_type,resource_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
