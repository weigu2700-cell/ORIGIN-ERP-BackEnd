-- EIP notification expansion. This script is safe to run repeatedly and never removes business rows.
-- It intentionally lives only under sql/migrations; do not duplicate it under sql/sql or sql/smart-erp.

CREATE TABLE IF NOT EXISTS `smart-erp`.sys_notification (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    business_id BIGINT NULL,
    business_no VARCHAR(100) NULL,
    business_type VARCHAR(100) NULL,
    publish_id BIGINT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    read_time DATETIME NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @eip_has_publish_id = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification' AND column_name = 'publish_id'
);
SET @eip_add_publish_id = IF(@eip_has_publish_id = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD COLUMN publish_id BIGINT NULL', 'SELECT 1');
PREPARE eip_add_publish_id_stmt FROM @eip_add_publish_id;
EXECUTE eip_add_publish_id_stmt;
DEALLOCATE PREPARE eip_add_publish_id_stmt;

SET @eip_has_publish_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification' AND index_name = 'idx_notification_publish_id'
);
SET @eip_add_publish_index = IF(@eip_has_publish_index = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD KEY idx_notification_publish_id (publish_id)', 'SELECT 1');
PREPARE eip_add_publish_index_stmt FROM @eip_add_publish_index;
EXECUTE eip_add_publish_index_stmt;
DEALLOCATE PREPARE eip_add_publish_index_stmt;

CREATE TABLE IF NOT EXISTS `smart-erp`.sys_notification_publish (
    id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NULL,
    publisher_id BIGINT NULL,
    source_type VARCHAR(20) NOT NULL,
    type TINYINT NOT NULL,
    template_id BIGINT NULL,
    merge_mode VARCHAR(20) NULL,
    notification_type TINYINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    business_type VARCHAR(100) NULL,
    business_id BIGINT NULL,
    business_no VARCHAR(100) NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_publish_request (request_id),
    UNIQUE KEY uk_notification_publish_idempotency (idempotency_key),
    KEY idx_notification_publish_template (template_id),
    KEY idx_notification_publish_business (business_type, business_id),
    KEY idx_notification_publish_publisher (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Upgrade installations that already have a publish table from an earlier EIP preview.
SET @eip_has_idempotency_key = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish' AND column_name = 'idempotency_key'
);
SET @eip_add_idempotency_key = IF(@eip_has_idempotency_key = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD COLUMN idempotency_key VARCHAR(100) NULL', 'SELECT 1');
PREPARE eip_add_idempotency_key_stmt FROM @eip_add_idempotency_key;
EXECUTE eip_add_idempotency_key_stmt;
DEALLOCATE PREPARE eip_add_idempotency_key_stmt;

SET @eip_has_publisher_id = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish' AND column_name = 'publisher_id'
);
SET @eip_add_publisher_id = IF(@eip_has_publisher_id = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD COLUMN publisher_id BIGINT NULL', 'SELECT 1');
PREPARE eip_add_publisher_id_stmt FROM @eip_add_publisher_id;
EXECUTE eip_add_publisher_id_stmt;
DEALLOCATE PREPARE eip_add_publisher_id_stmt;

SET @eip_has_request_id = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish' AND column_name = 'request_id'
);
SET @eip_add_request_id = IF(@eip_has_request_id = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD COLUMN request_id VARCHAR(100) NULL', 'SELECT 1');
PREPARE eip_add_request_id_stmt FROM @eip_add_request_id;
EXECUTE eip_add_request_id_stmt;
DEALLOCATE PREPARE eip_add_request_id_stmt;

SET @eip_has_source_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish' AND column_name = 'source_type'
);
SET @eip_add_source_type = IF(@eip_has_source_type = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD COLUMN source_type VARCHAR(20) NULL', 'SELECT 1');
PREPARE eip_add_source_type_stmt FROM @eip_add_source_type;
EXECUTE eip_add_source_type_stmt;
DEALLOCATE PREPARE eip_add_source_type_stmt;

SET @eip_has_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish' AND column_name = 'type'
);
SET @eip_add_type = IF(@eip_has_type = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD COLUMN type TINYINT NULL', 'SELECT 1');
PREPARE eip_add_type_stmt FROM @eip_add_type;
EXECUTE eip_add_type_stmt;
DEALLOCATE PREPARE eip_add_type_stmt;

CREATE TABLE IF NOT EXISTS `smart-erp`.sys_notification_template (
    id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    title_template VARCHAR(200) NOT NULL,
    content_template TEXT NOT NULL,
    notification_type TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500) NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_template_code (code),
    KEY idx_notification_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `smart-erp`.sys_notification_template_recipient (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    selector_type VARCHAR(32) NOT NULL,
    selector_value VARCHAR(200) NOT NULL,
    create_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_template_recipient (template_id, selector_type, selector_value),
    KEY idx_notification_template_recipient_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Permission seeds are conditional and therefore safe with existing installations.
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300001, '通知收件人选项', 'eip:notification:recipient:list', 2, NULL, 1, 300001, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:recipient:list');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300002, '发布通知', 'eip:notification:publish', 2, NULL, 1, 300002, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:publish');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300003, '通知模板列表', 'eip:notification:template:list', 2, NULL, 1, 300003, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:list');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300004, '通知模板详情', 'eip:notification:template:get', 2, NULL, 1, 300004, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:get');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300005, '创建通知模板', 'eip:notification:template:create', 2, NULL, 1, 300005, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:create');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300006, '修改通知模板', 'eip:notification:template:update', 2, NULL, 1, 300006, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:update');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300007, '通知模板状态', 'eip:notification:template:status', 2, NULL, 1, 300007, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:status');
INSERT INTO `smart-erp`.sys_permission (id, name, code, type, parent_id, status, sort, remark, create_time)
SELECT 300008, '删除通知模板', 'eip:notification:template:delete', 2, NULL, 1, 300008, 'EIP notification', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification:template:delete');
