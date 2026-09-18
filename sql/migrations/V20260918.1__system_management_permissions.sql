-- System management permissions.
-- sys_menu seeds bind the 系统管理 sub menus to system:user:list / system:role:list /
-- system:menu:list / system:permission:list / system:dept:list, but sys_permission never
-- contained any system:* row, so the permission-aware menu filter dropped every one of them.
-- This migration is safe to execute repeatedly:
--   * sys_permission IDs are allocated by MySQL AUTO_INCREMENT;
--   * permission code is the stable unique key;
--   * role mappings are inserted only when the pair does not already exist.

START TRANSACTION;

-- Root group for the system module.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
VALUES ('系统管理', 'system', 1, NULL, 1, 1, '系统管理权限组', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

-- Menu-bound permissions referenced by sys_menu.permission_code.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT seed.name,
       seed.code,
       2,
       parent.id,
       1,
       seed.sort,
       '系统管理菜单权限',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM (
    SELECT '部门-列表' AS name, 'system:dept:list' AS code, 1 AS sort
    UNION ALL
    SELECT '用户-列表', 'system:user:list', 2
    UNION ALL
    SELECT '角色-列表', 'system:role:list', 3
    UNION ALL
    SELECT '菜单-列表', 'system:menu:list', 4
    UNION ALL
    SELECT '权限-列表', 'system:permission:list', 5
) seed
JOIN `smart-erp`.sys_permission parent ON parent.code = 'system'
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
      'system',
      'system:dept:list',
      'system:user:list',
      'system:role:list',
      'system:menu:list',
      'system:permission:list'
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
