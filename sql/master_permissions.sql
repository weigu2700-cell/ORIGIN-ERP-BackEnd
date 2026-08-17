-- ============================================================
-- 主数据模块（master）权限初始化
-- 说明：补齐客户(customer)与供应商(supplier)的按钮级权限，
--       并将这些权限分配给 admin 角色(roleId = 1001)。
-- 编码规范：master:{模块}:{动作}
--   create 新增 / list 列表 / get 详情 / update 更新 / status 改状态
-- 约定：type=2(按钮) status=1(启用) sort 自增
-- 注意：id 使用 ASSIGN_ID，此处用固定占位 id 便于幂等，
--       若你的环境 id 由数据库/雪花生成，请删除 id 列让其自增。
-- ============================================================

-- ---------- 客户权限（如已存在可跳过） ----------
INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3001, '客户新增', 'master:customer:create', 2, NULL, 1, 1, '主数据-客户', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:create');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3002, '客户列表', 'master:customer:list', 2, NULL, 2, 1, '主数据-客户', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:list');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3003, '客户详情', 'master:customer:get', 2, NULL, 3, 1, '主数据-客户', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:get');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3004, '客户更新', 'master:customer:update', 2, NULL, 4, 1, '主数据-客户', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:update');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3009, '客户状态', 'master:customer:status', 2, NULL, 5, 1, '主数据-客户', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:status');

-- ---------- 供应商权限（本次新增） ----------
INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3005, '供应商新增', 'master:supplier:create', 2, NULL, 6, 1, '主数据-供应商', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:create');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3006, '供应商列表', 'master:supplier:list', 2, NULL, 7, 1, '主数据-供应商', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:list');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3007, '供应商详情', 'master:supplier:get', 2, NULL, 8, 1, '主数据-供应商', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:get');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3008, '供应商更新', 'master:supplier:update', 2, NULL, 9, 1, '主数据-供应商', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:update');

INSERT INTO sys_permission (id, name, code, type, parent_id, sort, status, remark, deleted)
SELECT 3010, '供应商状态', 'master:supplier:status', 2, NULL, 10, 1, '主数据-供应商', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:status');

-- ---------- 将以上权限分配给 admin 角色(1001) ----------
-- 客户权限 3001~3004, 3009
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1001, p.id FROM sys_permission p
WHERE p.code IN (
    'master:customer:create', 'master:customer:list', 'master:customer:get',
    'master:customer:update', 'master:customer:status'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 1001 AND rp.permission_id = p.id
);

-- 供应商权限 3005~3008, 3010
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1001, p.id FROM sys_permission p
WHERE p.code IN (
    'master:supplier:create', 'master:supplier:list', 'master:supplier:get',
    'master:supplier:update', 'master:supplier:status'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 1001 AND rp.permission_id = p.id
);
