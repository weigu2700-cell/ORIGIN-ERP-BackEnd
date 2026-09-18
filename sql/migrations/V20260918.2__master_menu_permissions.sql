-- Master data (基础资料) menu permissions.
-- The permission-aware menu filter drops any menu whose permission_code has no matching
-- sys_permission row, so 基础资料 was missing 工厂 / 车间 / 生产线 / 物料供应商.
-- Causes:
--   1. 工厂 / 生产线 menus used a code that differs from the backend @PreAuthorize authority
--      (mobile/src/core/auth/permission-registry.ts pins the backend code as the contract,
--      so the menu side is aligned to the backend).
--   2. 车间 has no permission at all and WorkshopController has no @PreAuthorize.
--   3. 物料供应商 is missing the list permission.
-- Safe to execute repeatedly: codes are the stable key, and role grants are de-duplicated.

START TRANSACTION;

-- 1) Align menu codes with the backend authorities.
UPDATE `smart-erp`.sys_menu
SET permission_code = 'factory:list',
    update_time = CURRENT_TIMESTAMP
WHERE name = 'factory'
  AND deleted = 0
  AND permission_code <> 'factory:list';

UPDATE `smart-erp`.sys_menu
SET permission_code = 'master:production_line:list',
    update_time = CURRENT_TIMESTAMP
WHERE name = 'production-line'
  AND deleted = 0
  AND permission_code <> 'master:production_line:list';

-- 2) 车间 permission group.
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
VALUES ('车间', 'master:workshop', 1, 1, 1, 6, '车间管理权限组', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT seed.name,
       seed.code,
       2,
       parent.id,
       1,
       seed.sort,
       '车间操作权限',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM (
    SELECT '车间-列表' AS name, 'master:workshop:list' AS code, 1 AS sort
    UNION ALL
    SELECT '车间-新增', 'master:workshop:create', 2
    UNION ALL
    SELECT '车间-详情', 'master:workshop:get', 3
    UNION ALL
    SELECT '车间-修改', 'master:workshop:update', 4
    UNION ALL
    SELECT '车间-状态变更', 'master:workshop:status', 5
) seed
JOIN `smart-erp`.sys_permission parent ON parent.code = 'master:workshop'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

-- 3) 物料供应商-列表。
INSERT INTO `smart-erp`.sys_permission
    (name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
SELECT '物料供应商-列表',
       'master:material-supplier:list',
       2,
       parent.id,
       1,
       1,
       '物料供应商列表权限',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM `smart-erp`.sys_permission parent
WHERE parent.code = 'master:material-supplier'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    status = VALUES(status),
    sort = VALUES(sort),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

-- 4) Grant the administrator role the new codes.
INSERT INTO `smart-erp`.sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), role.id, permission.id
FROM `smart-erp`.sys_role role
JOIN `smart-erp`.sys_permission permission
  ON permission.code IN (
      'master:workshop',
      'master:workshop:list',
      'master:workshop:create',
      'master:workshop:get',
      'master:workshop:update',
      'master:workshop:status',
      'master:material-supplier:list'
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
