-- Wave 0: mobile production/inventory permissions.
-- This migration is safe to execute repeatedly:
--   * sys_permission IDs are allocated by MySQL AUTO_INCREMENT;
--   * permission code is the stable unique key;
--   * role mappings are inserted only when the pair does not already exist.

START TRANSACTION;

-- Add the three permission groups below existing business roots first.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT seed.name,
       seed.code,
       1,
       parent.id,
       1,
       seed.sort,
       '移动端 Wave 0 权限',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM (
    SELECT '生产领料' AS name, 'production:picking' AS code, 'production' AS parent_code, 1 AS sort
    UNION ALL
    SELECT '生产报工', 'prd:report', 'production', 2
    UNION ALL
    SELECT '成品入库', 'inv:finish-warehousing', 'inventory', 1
) seed
JOIN `smart-erp`.sys_permission parent ON parent.code = seed.parent_code
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

-- Add every action currently enforced by the three mobile-facing controllers.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT seed.name,
       seed.code,
       2,
       parent.id,
       1,
       seed.sort,
       '移动端 Wave 0 权限',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM (
    SELECT '领料单-列表' AS name, 'production:picking:list' AS code, 'production:picking' AS parent_code, 1 AS sort
    UNION ALL
    SELECT '领料单-详情', 'production:picking:get', 'production:picking', 2
    UNION ALL
    SELECT '领料单-审核', 'production:picking:approve', 'production:picking', 3
    UNION ALL
    SELECT '领料单-确认', 'production:picking:confirm', 'production:picking', 4
    UNION ALL
    SELECT '报工单-新增', 'prd:report:add', 'prd:report', 1
    UNION ALL
    SELECT '报工单-列表', 'prd:report:page', 'prd:report', 2
    UNION ALL
    SELECT '报工单-详情', 'prd:report:get', 'prd:report', 3
    UNION ALL
    SELECT '报工单-审批', 'prd:report:approve', 'prd:report', 4
    UNION ALL
    SELECT '报工单-取消', 'prd:report:cancel', 'prd:report', 5
    UNION ALL
    SELECT '报工单-驳回', 'prd:report:reject', 'prd:report', 6
    UNION ALL
    SELECT '报工单-完成', 'prd:report:finish', 'prd:report', 7
    UNION ALL
    SELECT '成品入库-列表', 'inv:finish-warehousing:list', 'inv:finish-warehousing', 1
    UNION ALL
    SELECT '成品入库-详情', 'inv:finish-warehousing:get', 'inv:finish-warehousing', 2
    UNION ALL
    SELECT '成品入库-操作', 'inv:finish-warehousing:update', 'inv:finish-warehousing', 3
) seed
JOIN `smart-erp`.sys_permission parent ON parent.code = seed.parent_code
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

-- Keep the administrator role complete without assuming a fixed role or permission ID.
INSERT INTO `smart-erp`.sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), role.id, permission.id
FROM `smart-erp`.sys_role role
JOIN `smart-erp`.sys_permission permission
  ON permission.code IN (
      'production:picking',
      'production:picking:list',
      'production:picking:get',
      'production:picking:approve',
      'production:picking:confirm',
      'prd:report',
      'prd:report:add',
      'prd:report:page',
      'prd:report:get',
      'prd:report:approve',
      'prd:report:cancel',
      'prd:report:reject',
      'prd:report:finish',
      'inv:finish-warehousing',
      'inv:finish-warehousing:list',
      'inv:finish-warehousing:get',
      'inv:finish-warehousing:update'
  )
WHERE role.code = 'admin'
  AND role.deleted = 0
  AND permission.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `smart-erp`.sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

COMMIT;
