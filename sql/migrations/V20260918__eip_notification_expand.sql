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
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_publish_user (publish_id, user_id),
    KEY idx_notification_inbox (user_id, is_read, create_time, id),
    KEY idx_notification_business (business_type, business_id)
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

SET @eip_has_publish_user_key = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification'
      AND index_name = 'uk_notification_publish_user'
);
SET @eip_add_publish_user_key = IF(@eip_has_publish_user_key = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD UNIQUE KEY uk_notification_publish_user (publish_id, user_id)',
    'SELECT 1');
PREPARE eip_add_publish_user_key_stmt FROM @eip_add_publish_user_key;
EXECUTE eip_add_publish_user_key_stmt;
DEALLOCATE PREPARE eip_add_publish_user_key_stmt;

SET @eip_has_inbox_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification'
      AND index_name = 'idx_notification_inbox'
);
SET @eip_add_inbox_index = IF(@eip_has_inbox_index = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD KEY idx_notification_inbox (user_id, is_read, create_time, id)',
    'SELECT 1');
PREPARE eip_add_inbox_index_stmt FROM @eip_add_inbox_index;
EXECUTE eip_add_inbox_index_stmt;
DEALLOCATE PREPARE eip_add_inbox_index_stmt;

SET @eip_has_business_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification'
      AND index_name = 'idx_notification_business'
);
SET @eip_add_business_index = IF(@eip_has_business_index = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD KEY idx_notification_business (business_type, business_id)',
    'SELECT 1');
PREPARE eip_add_business_index_stmt FROM @eip_add_business_index;
EXECUTE eip_add_business_index_stmt;
DEALLOCATE PREPARE eip_add_business_index_stmt;

CREATE TABLE IF NOT EXISTS `smart-erp`.sys_notification_publish (
    id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    type TINYINT NOT NULL,
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
    KEY idx_notification_publish_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Upgrade installations that already have a publish table from an earlier EIP preview.
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

SET @eip_has_publish_request_key = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish'
      AND index_name = 'uk_notification_publish_request'
);
SET @eip_add_publish_request_key = IF(@eip_has_publish_request_key = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD UNIQUE KEY uk_notification_publish_request (request_id)',
    'SELECT 1');
PREPARE eip_add_publish_request_key_stmt FROM @eip_add_publish_request_key;
EXECUTE eip_add_publish_request_key_stmt;
DEALLOCATE PREPARE eip_add_publish_request_key_stmt;

SET @eip_has_publish_business_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'smart-erp' AND table_name = 'sys_notification_publish'
      AND index_name = 'idx_notification_publish_business'
);
SET @eip_add_publish_business_index = IF(@eip_has_publish_business_index = 0,
    'ALTER TABLE `smart-erp`.sys_notification_publish ADD KEY idx_notification_publish_business (business_type, business_id)',
    'SELECT 1');
PREPARE eip_add_publish_business_index_stmt FROM @eip_add_publish_business_index;
EXECUTE eip_add_publish_business_index_stmt;
DEALLOCATE PREPARE eip_add_publish_business_index_stmt;

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
    include_children TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_template_recipient (template_id, selector_type, selector_value),
    KEY idx_notification_template_recipient_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @eip_has_recipient_include_children = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'smart-erp'
      AND table_name = 'sys_notification_template_recipient'
      AND column_name = 'include_children'
);
SET @eip_add_recipient_include_children = IF(@eip_has_recipient_include_children = 0,
    'ALTER TABLE `smart-erp`.sys_notification_template_recipient ADD COLUMN include_children TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE eip_add_recipient_include_children_stmt FROM @eip_add_recipient_include_children;
EXECUTE eip_add_recipient_include_children_stmt;
DEALLOCATE PREPARE eip_add_recipient_include_children_stmt;

-- Permission seeds are conditional and therefore safe with existing installations.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT '消息通知', 'eip:notification', 1, NULL, 1, 1, 'EIP notification', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `smart-erp`.sys_permission WHERE code = 'eip:notification'
);

INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT seed.name, seed.code, 2, parent.id, 1, seed.sort, 'EIP notification', NOW(), NOW(), 0
FROM (
    SELECT '通知收件人选项' AS name, 'eip:notification:recipient:list' AS code, 1 AS sort
    UNION ALL SELECT '发布通知', 'eip:notification:publish', 2
    UNION ALL SELECT '通知模板列表', 'eip:notification:template:list', 3
    UNION ALL SELECT '通知模板详情', 'eip:notification:template:get', 4
    UNION ALL SELECT '创建通知模板', 'eip:notification:template:create', 5
    UNION ALL SELECT '修改通知模板', 'eip:notification:template:update', 6
    UNION ALL SELECT '通知模板状态', 'eip:notification:template:status', 7
    UNION ALL SELECT '删除通知模板', 'eip:notification:template:delete', 8
) seed
JOIN `smart-erp`.sys_permission parent ON parent.code = 'eip:notification'
LEFT JOIN `smart-erp`.sys_permission existing_permission ON existing_permission.code = seed.code
WHERE existing_permission.id IS NULL;

INSERT INTO `smart-erp`.sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), role.id, permission.id
FROM `smart-erp`.sys_role role
JOIN `smart-erp`.sys_permission permission
  ON permission.code = 'eip:notification'
  OR permission.code LIKE 'eip:notification:%'
LEFT JOIN `smart-erp`.sys_role_permission existing_mapping
  ON existing_mapping.role_id = role.id
 AND existing_mapping.permission_id = permission.id
WHERE role.code = 'admin'
  AND existing_mapping.id IS NULL;
