ALTER TABLE sys_user
    ADD COLUMN real_name varchar(50) NULL COMMENT '真实姓名' AFTER username;

UPDATE sys_user
SET real_name = username
WHERE real_name IS NULL OR TRIM(real_name) = '';
