CREATE TABLE plt_tenant_operation (
 id VARCHAR(36) PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 action VARCHAR(16) NOT NULL,
 phase VARCHAR(24) NOT NULL,
 status VARCHAR(16) NOT NULL,
 version_before VARCHAR(64),
 version_after VARCHAR(64),
 error_code VARCHAR(64),
 started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at TIMESTAMP NULL,
 FOREIGN KEY (tenant_id) REFERENCES plt_tenant(id),
 INDEX idx_tenant_operation (tenant_id, started_at),
 CHECK (action IN ('NEW','ADOPT','UPGRADE')),
 CHECK (status IN ('RUNNING','SUCCEEDED','FAILED'))
);
