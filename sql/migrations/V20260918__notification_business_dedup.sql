-- 业务通知去重约束：同一用户、同一业务类型和业务 ID 只保留一条通知。
-- 通用系统通知允许 business_type/business_id 为空，不纳入业务幂等键。
-- 执行前请确认 sys_notification 已由基础库初始化。

DELETE duplicate_notification
FROM `smart-erp`.sys_notification duplicate_notification
JOIN `smart-erp`.sys_notification kept_notification
  ON kept_notification.id < duplicate_notification.id
 AND kept_notification.user_id = duplicate_notification.user_id
 AND kept_notification.business_type = duplicate_notification.business_type
 AND kept_notification.business_id = duplicate_notification.business_id
WHERE duplicate_notification.business_type IS NOT NULL
  AND duplicate_notification.business_id IS NOT NULL;

SET @notification_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = 'smart-erp'
      AND table_name = 'sys_notification'
      AND index_name = 'uk_notification_user_business'
);

SET @notification_index_sql = IF(
    @notification_index_exists = 0,
    'ALTER TABLE `smart-erp`.sys_notification ADD UNIQUE KEY uk_notification_user_business (user_id, business_type, business_id)',
    'SELECT 1'
);
PREPARE notification_index_statement FROM @notification_index_sql;
EXECUTE notification_index_statement;
DEALLOCATE PREPARE notification_index_statement;
