-- 显式选择独立平台库后执行；不要导入任何租户业务库。
CREATE TABLE plt_tenant (
 id VARCHAR(64) PRIMARY KEY,
 code VARCHAR(64) NOT NULL UNIQUE,
 name VARCHAR(128) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PROVISIONING',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK (status IN ('PROVISIONING','ACTIVE','FAILED','SUSPENDED'))
);
CREATE TABLE plt_tenant_database (
 tenant_id VARCHAR(64) PRIMARY KEY,
 connection_ref VARCHAR(64) NOT NULL UNIQUE,
 routing_version BIGINT NOT NULL DEFAULT 1,
 schema_version VARCHAR(64),
 schema_status VARCHAR(16) NOT NULL DEFAULT 'UPGRADING',
 FOREIGN KEY (tenant_id) REFERENCES plt_tenant(id),
 CHECK (routing_version > 0),
 CHECK (schema_status IN ('READY','UPGRADING','FAILED'))
);
