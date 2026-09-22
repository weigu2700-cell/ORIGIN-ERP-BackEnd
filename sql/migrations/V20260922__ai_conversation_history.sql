-- Application-facing conversation history. Spring AI's JDBC ChatMemory uses its own table.
-- IDs are assigned by MyBatis-Plus (ASSIGN_ID), so these columns do not auto-increment.
CREATE TABLE IF NOT EXISTS `smart-erp`.ai_conversation (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user (user_id, deleted, update_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `smart-erp`.ai_message (
    id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation (conversation_id, deleted, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
