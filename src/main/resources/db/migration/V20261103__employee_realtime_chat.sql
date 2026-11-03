-- IM 使用租户数据源；tenant_id 同时防止错误上下文读取相同数值 ID。
CREATE TABLE im_conversation (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 type VARCHAR(12) NOT NULL,
 name VARCHAR(256) NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 owner_id BIGINT NULL,
 creator_id BIGINT NOT NULL,
 client_request_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 create_payload_hash CHAR(64) NULL,
 direct_user_low BIGINT NULL,
 direct_user_high BIGINT NULL,
 version INT NOT NULL DEFAULT 1,
 last_seq BIGINT NOT NULL DEFAULT 0,
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_im_direct (tenant_id,direct_user_low,direct_user_high),
 UNIQUE KEY uq_im_group_request (tenant_id,creator_id,client_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_member (
 tenant_id VARCHAR(64) NOT NULL,
 conversation_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 role VARCHAR(12) NULL,
 state VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
 visible_from_seq BIGINT NOT NULL,
 last_read_seq BIGINT NOT NULL DEFAULT 0,
 unread_count INT NOT NULL DEFAULT 0,
 muted TINYINT NOT NULL DEFAULT 0,
 joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 left_at TIMESTAMP(6) NULL,
 PRIMARY KEY (tenant_id,conversation_id,user_id),
 KEY ix_im_member_user (tenant_id,user_id,state,conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_member_change (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 conversation_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 target_id BIGINT NULL,
 action VARCHAR(32) NOT NULL,
 from_role VARCHAR(12) NULL,
 to_role VARCHAR(12) NULL,
 seq BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY ix_im_member_audit (tenant_id,conversation_id,seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_message (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 conversation_id BIGINT NOT NULL,
 seq BIGINT NOT NULL,
 sender_id BIGINT NOT NULL,
 client_message_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 payload_hash CHAR(64) NOT NULL,
 type VARCHAR(12) NOT NULL,
 text TEXT NULL,
 file_id BIGINT NULL,
 system_payload JSON NULL,
 sender_event_seq BIGINT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_im_message_seq (tenant_id,conversation_id,seq),
 UNIQUE KEY uq_im_message_client (tenant_id,sender_id,client_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_upload (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 client_upload_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 payload_hash CHAR(64) NOT NULL,
 conversation_id BIGINT NOT NULL,
 uploader_id BIGINT NOT NULL,
 object_key VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 object_version_id VARCHAR(1024) NULL,
 name VARCHAR(255) NOT NULL,
 declared_size INT NOT NULL,
 mime VARCHAR(255) NOT NULL,
 sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'INITIATED',
 rejection_reason VARCHAR(64) NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 signed_until TIMESTAMP(6) NOT NULL,
 lease_until TIMESTAMP(6) NULL,
 lease_owner VARCHAR(36) NULL,
 next_attempt_at TIMESTAMP(6) NULL,
 cleaned TINYINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_im_upload_client (tenant_id,uploader_id,client_upload_id),
 UNIQUE KEY uq_im_upload_key (object_key),
 KEY ix_im_upload_worker (tenant_id,state,next_attempt_at,lease_until),
 KEY ix_im_upload_expiry (tenant_id,expires_at,cleaned),
 KEY ix_im_upload_user (tenant_id,uploader_id,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_file (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 upload_id BIGINT NOT NULL,
 conversation_id BIGINT NOT NULL,
 uploader_id BIGINT NOT NULL,
 message_id BIGINT NULL,
 object_key VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 object_version_id VARCHAR(1024) NOT NULL,
 name VARCHAR(255) NOT NULL,
 size INT NOT NULL,
 mime VARCHAR(255) NOT NULL,
 sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'READY',
 UNIQUE KEY uq_im_file_upload (tenant_id,upload_id),
 UNIQUE KEY uq_im_file_message (tenant_id,message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_storage_budget (
 tenant_id VARCHAR(64) NOT NULL PRIMARY KEY,
 reserved_bytes BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 type VARCHAR(32) NOT NULL,
 conversation_id BIGINT NOT NULL,
 visibility_seq BIGINT NOT NULL,
 payload JSON NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY ix_im_event_retention (tenant_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_user_stream (
 tenant_id VARCHAR(64) NOT NULL,
 user_id BIGINT NOT NULL,
 last_seq BIGINT NOT NULL DEFAULT 0,
 pruned_through_seq BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY (tenant_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_user_event (
 tenant_id VARCHAR(64) NOT NULL,
 user_id BIGINT NOT NULL,
 event_seq BIGINT NOT NULL,
 event_id BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (tenant_id,user_id,event_seq),
 UNIQUE KEY uq_im_recipient_event (tenant_id,user_id,event_id),
 KEY ix_im_user_event_retention (tenant_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE im_outbox (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL,
 event_id BIGINT NOT NULL,
 recipient_user_id BIGINT NOT NULL,
 state VARCHAR(12) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 lease_owner VARCHAR(36) NULL,
 lease_until TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY ix_im_outbox_poll (tenant_id,state,next_attempt_at,lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_permission (name,code,type,status,sort,create_time,deleted)
SELECT '员工聊天','im:access',2,1,0,NOW(),0 WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code='im:access');
INSERT INTO sys_permission (name,code,type,status,sort,create_time,deleted)
SELECT '创建群聊','im:group:create',2,1,0,NOW(),0 WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code='im:group:create');
INSERT INTO sys_permission (name,code,type,status,sort,create_time,deleted)
SELECT '聊天附件','im:file:upload',2,1,0,NOW(),0 WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code='im:file:upload');
INSERT INTO sys_role_permission (id,role_id,permission_id)
SELECT UUID_SHORT(),r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code='admin' AND r.status=1 AND r.deleted=0 AND p.code IN ('im:access','im:group:create','im:file:upload')
 AND p.status=1 AND p.deleted=0 AND NOT EXISTS
 (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
