-- =============================================================================
-- 权限全量初始化脚本（菜单 + 按钮，含父子关系）
--
-- 结构：模块菜单(type=1) -> 资源菜单(type=1) -> 操作按钮(type=2)
-- 取值：type 1=菜单 / 2=按钮（PermissionType）；status 1=启用（Status）
-- 说明：
--   1) sys_permission.id 是雪花 ID 无自增，这里按 1..N 显式生成，保证父子引用可控
--   2) 先插入全部行（parent_id 暂为 NULL），再按 code 映射回填 parent_id
--   3) 最后把全部权限一次性绑给超级管理员（role_id = 1）
--   4) 脚本可重复执行；按钮 code 与后端 @PreAuthorize 完全一致
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. 清空（谨慎：会物理删除现有权限及角色绑定）
-- -----------------------------------------------------------------------------
DELETE FROM sys_role_permission;
DELETE FROM sys_permission;

-- -----------------------------------------------------------------------------
-- 1. 插入全部权限行（parent_id 稍后回填）
-- -----------------------------------------------------------------------------
INSERT INTO sys_permission (id, name, code, type, parent_id, status, sort, create_time, deleted)
SELECT ROW_NUMBER() OVER (ORDER BY t.ord),
       t.name,
       t.code,
       t.type,
       NULL,
       1,
       t.ord,
       NOW(),
       0
FROM (
    -- ===== 主数据管理 =====
    SELECT 1  AS ord, '主数据管理'   AS name, 'master'                          AS code, 1 AS type
    UNION ALL SELECT 2,  '客户管理',            'master:customer',                     1
    UNION ALL SELECT 3,  '客户-新增',          'master:customer:create',               2
    UNION ALL SELECT 4,  '客户-列表',          'master:customer:list',                 2
    UNION ALL SELECT 5,  '客户-详情',          'master:customer:get',                  2
    UNION ALL SELECT 6,  '客户-修改',          'master:customer:update',               2
    UNION ALL SELECT 7,  '客户-状态变更',      'master:customer:status',               2
    UNION ALL SELECT 8,  '客户-导出',          'master:customer:export',               2

    UNION ALL SELECT 9,  '供应商管理',         'master:supplier',                      1
    UNION ALL SELECT 10, '供应商-新增',        'master:supplier:create',               2
    UNION ALL SELECT 11, '供应商-列表',        'master:supplier:list',                 2
    UNION ALL SELECT 12, '供应商-详情',        'master:supplier:get',                  2
    UNION ALL SELECT 13, '供应商-修改',        'master:supplier:update',               2
    UNION ALL SELECT 14, '供应商-状态变更',    'master:supplier:status',               2
    UNION ALL SELECT 15, '供应商-导出',        'master:supplier:export',               2

    UNION ALL SELECT 16, '物料管理',           'master:material',                      1
    UNION ALL SELECT 17, '物料-新增',          'master:material:create',               2
    UNION ALL SELECT 18, '物料-列表',          'master:material:list',                 2
    UNION ALL SELECT 19, '物料-详情',          'master:material:get',                  2
    UNION ALL SELECT 20, '物料-修改',          'master:material:update',               2
    UNION ALL SELECT 21, '物料-状态变更',      'master:material:status',               2
    UNION ALL SELECT 22, '物料-导出',          'master:material:export',               2

    UNION ALL SELECT 23, '物料供应商',         'master:material-supplier',             1
    UNION ALL SELECT 24, '物料供应商-新增',    'master:material-supplier:create',      2
    UNION ALL SELECT 25, '物料供应商-修改',    'master:material-supplier:update',      2
    UNION ALL SELECT 26, '物料供应商-状态变更','master:material-supplier:status',      2

    UNION ALL SELECT 27, '生产线管理',         'master:production_line',               1
    UNION ALL SELECT 28, '生产线-新增',        'master:production_line:create',        2
    UNION ALL SELECT 29, '生产线-列表',        'master:production_line:list',          2
    UNION ALL SELECT 30, '生产线-详情',        'master:production_line:get',           2
    UNION ALL SELECT 31, '生产线-修改',        'master:production_line:update',        2
    UNION ALL SELECT 32, '生产线-状态变更',    'master:production_line:status',        2
    UNION ALL SELECT 33, '生产线-导出',        'master:production_line:export',        2

    UNION ALL SELECT 34, '仓库管理',           'master:warehouse',                     1
    UNION ALL SELECT 35, '仓库-新增',          'master:warehouse:create',              2
    UNION ALL SELECT 36, '仓库-列表',          'master:warehouse:list',                2
    UNION ALL SELECT 37, '仓库-详情',          'master:warehouse:get',                 2
    UNION ALL SELECT 38, '仓库-修改',          'master:warehouse:update',              2
    UNION ALL SELECT 39, '仓库-状态变更',      'master:warehouse:status',              2

    UNION ALL SELECT 40, '工厂管理',           'factory',                              1
    UNION ALL SELECT 41, '工厂-新增',          'factory:create',                       2
    UNION ALL SELECT 42, '工厂-列表',          'factory:list',                         2
    UNION ALL SELECT 43, '工厂-详情',          'factory:get',                          2
    UNION ALL SELECT 44, '工厂-修改',          'factory:update',                       2
    UNION ALL SELECT 45, '工厂-状态变更',      'factory:status:update',                2
    UNION ALL SELECT 46, '工厂-导出',          'factory:export',                       2

    -- ===== 采购管理 =====
    UNION ALL SELECT 47, '采购管理',           'purchase',                             1
    UNION ALL SELECT 48, '采购需求',           'purchase:demand',                      1
    UNION ALL SELECT 49, '采购需求-新增',      'purchase:demand:create',               2
    UNION ALL SELECT 50, '采购需求-列表',      'purchase:demand:list',                 2
    UNION ALL SELECT 51, '采购需求-详情',      'purchase:demand:get',                  2
    UNION ALL SELECT 52, '采购需求-审批',      'purchase:demand:approve',              2
    UNION ALL SELECT 53, '采购需求-关闭',      'purchase:demand:close',                2

    UNION ALL SELECT 54, '采购订单',           'purchase:order',                       1
    UNION ALL SELECT 55, '采购订单-新增',      'purchase:order:create',                2
    UNION ALL SELECT 56, '采购订单-列表',      'purchase:order:list',                  2
    UNION ALL SELECT 57, '采购订单-详情',      'purchase:order:get',                   2
    UNION ALL SELECT 58, '采购订单-修改',      'purchase:order:update',                2
    UNION ALL SELECT 59, '采购订单-审批',      'purchase:order:approve',               2
    UNION ALL SELECT 60, '采购订单-发货',      'purchase:order:ship',                  2
    UNION ALL SELECT 61, '采购订单-收货',      'purchase:order:receive',               2
    UNION ALL SELECT 62, '采购订单-关闭',      'purchase:order:close',                 2

    UNION ALL SELECT 63, '采购入库',           'purchase:in:stock',                    1
    UNION ALL SELECT 64, '采购入库-列表',      'purchase:in:stock:list',               2
    UNION ALL SELECT 65, '采购入库-审核',      'purchase:in:stock:approve',            2
    UNION ALL SELECT 66, '采购入库-上架',      'purchase:in:stock:upload',             2

    -- ===== 销售管理 =====
    UNION ALL SELECT 67, '销售管理',           'sales',                                1
    UNION ALL SELECT 68, '销售订单',           'sales:order',                          1
    UNION ALL SELECT 69, '销售订单-新增',      'sales:order:create',                   2
    UNION ALL SELECT 70, '销售订单-列表',      'sales:order:list',                     2
    UNION ALL SELECT 71, '销售订单-详情',      'sales:order:get',                      2
    UNION ALL SELECT 72, '销售订单-修改',      'sales:order:update',                   2
    UNION ALL SELECT 73, '销售订单-删除',      'sales:order:delete',                   2
    UNION ALL SELECT 74, '销售订单-确认',      'sales:order:confirm',                  2
    UNION ALL SELECT 75, '销售订单-取消',      'sales:order:cancel',                   2

    UNION ALL SELECT 76, '销售出库',           'sales:delivery',                       1
    UNION ALL SELECT 77, '销售出库-新增',      'sales:delivery:create',                2
    UNION ALL SELECT 78, '销售出库-列表',      'sales:delivery:list',                  2
    UNION ALL SELECT 79, '销售出库-详情',      'sales:delivery:get',                   2
    UNION ALL SELECT 80, '销售出库-确认',      'sales:delivery:confirm',               2
    UNION ALL SELECT 81, '销售出库-完成',      'sales:delivery:complete',              2
    UNION ALL SELECT 82, '销售出库-取消',      'sales:delivery:cancel',                2

    -- ===== 生产管理 =====
    UNION ALL SELECT 83, '生产管理',           'production',                           1
    UNION ALL SELECT 84, 'BOM管理',            'production:bom',                       1
    UNION ALL SELECT 85, 'BOM-新增',           'production:bom:create',                2
    UNION ALL SELECT 86, 'BOM-详情',           'production:bom:get',                   2
    UNION ALL SELECT 87, 'BOM-列表',           'production:bom:list',                  2
    UNION ALL SELECT 88, 'BOM-修改',           'production:bom:update',                2
    UNION ALL SELECT 89, 'BOM-禁用',           'production:bom:disable',               2
    UNION ALL SELECT 90, 'BOM-展开',           'production:bom:explosion',             2
    UNION ALL SELECT 91, 'BOM-需求计算',       'production:bom:requirement',           2

    UNION ALL SELECT 92, '生产需求',           'production:demand',                    1
    UNION ALL SELECT 93, '生产需求-列表',      'production:demand:list',               2
    UNION ALL SELECT 94, '生产需求-详情',      'production:demand:get',                2

    UNION ALL SELECT 95,  '生产订单',          'production:order',                     1
    UNION ALL SELECT 96,  '生产订单-新增',     'production:order:create',              2
    UNION ALL SELECT 97,  '生产订单-列表',     'production:order:list',                2
    UNION ALL SELECT 98,  '生产订单-详情',     'production:order:get',                 2
    UNION ALL SELECT 99,  '生产订单-下达',     'production:order:release',             2
    UNION ALL SELECT 100, '生产订单-开工',     'production:order:start',               2
    UNION ALL SELECT 101, '生产订单-完成',     'production:order:complete',            2
    UNION ALL SELECT 102, '生产订单-取消',     'production:order:cancel',              2

    -- ===== 库存管理 =====
    UNION ALL SELECT 103, '库存管理',          'inventory',                            1
    UNION ALL SELECT 104, '物料库存',          'inventory:material-stock',             1
    UNION ALL SELECT 105, '物料库存-新增',     'inventory:material-stock:create',      2
    UNION ALL SELECT 106, '物料库存-列表',     'inventory:material-stock:list',        2
    UNION ALL SELECT 107, '物料库存-详情',     'inventory:material-stock:get',         2

    UNION ALL SELECT 108, '库存流水',          'inventory:transaction',                1
    UNION ALL SELECT 109, '库存流水-列表',     'inventory:transaction:list',           2
    UNION ALL SELECT 110, '库存流水-导出',     'inventory:transaction:export',         2
    UNION ALL SELECT 111, '库存流水-导入',     'inventory:transaction:import',         2
) t;

-- -----------------------------------------------------------------------------
-- 2. 回填 parent_id（按 code 映射父子关系）
-- -----------------------------------------------------------------------------
UPDATE sys_permission p
    JOIN (
        -- 二级资源菜单 -> 一级模块菜单
        SELECT 'master:customer'          AS code, 'master'     AS parent_code
        UNION ALL SELECT 'master:supplier',         'master'
        UNION ALL SELECT 'master:material',         'master'
        UNION ALL SELECT 'master:material-supplier','master'
        UNION ALL SELECT 'master:production_line',  'master'
        UNION ALL SELECT 'master:warehouse',        'master'
        UNION ALL SELECT 'factory',                 'master'
        UNION ALL SELECT 'purchase:demand',         'purchase'
        UNION ALL SELECT 'purchase:order',          'purchase'
        UNION ALL SELECT 'purchase:in:stock',       'purchase'
        UNION ALL SELECT 'sales:order',             'sales'
        UNION ALL SELECT 'sales:delivery',          'sales'
        UNION ALL SELECT 'production:bom',          'production'
        UNION ALL SELECT 'production:demand',       'production'
        UNION ALL SELECT 'production:order',        'production'
        UNION ALL SELECT 'inventory:material-stock','inventory'
        UNION ALL SELECT 'inventory:transaction',   'inventory'
        -- 三级操作按钮 -> 二级资源菜单
        UNION ALL SELECT 'master:customer:create',            'master:customer'
        UNION ALL SELECT 'master:customer:list',              'master:customer'
        UNION ALL SELECT 'master:customer:get',               'master:customer'
        UNION ALL SELECT 'master:customer:update',            'master:customer'
        UNION ALL SELECT 'master:customer:status',            'master:customer'
        UNION ALL SELECT 'master:customer:export',            'master:customer'
        UNION ALL SELECT 'master:supplier:create',            'master:supplier'
        UNION ALL SELECT 'master:supplier:list',              'master:supplier'
        UNION ALL SELECT 'master:supplier:get',               'master:supplier'
        UNION ALL SELECT 'master:supplier:update',            'master:supplier'
        UNION ALL SELECT 'master:supplier:status',            'master:supplier'
        UNION ALL SELECT 'master:supplier:export',            'master:supplier'
        UNION ALL SELECT 'master:material:create',            'master:material'
        UNION ALL SELECT 'master:material:list',              'master:material'
        UNION ALL SELECT 'master:material:get',               'master:material'
        UNION ALL SELECT 'master:material:update',            'master:material'
        UNION ALL SELECT 'master:material:status',            'master:material'
        UNION ALL SELECT 'master:material:export',            'master:material'
        UNION ALL SELECT 'master:material-supplier:create',   'master:material-supplier'
        UNION ALL SELECT 'master:material-supplier:update',   'master:material-supplier'
        UNION ALL SELECT 'master:material-supplier:status',   'master:material-supplier'
        UNION ALL SELECT 'master:production_line:create',     'master:production_line'
        UNION ALL SELECT 'master:production_line:list',       'master:production_line'
        UNION ALL SELECT 'master:production_line:get',        'master:production_line'
        UNION ALL SELECT 'master:production_line:update',     'master:production_line'
        UNION ALL SELECT 'master:production_line:status',     'master:production_line'
        UNION ALL SELECT 'master:production_line:export',     'master:production_line'
        UNION ALL SELECT 'master:warehouse:create',           'master:warehouse'
        UNION ALL SELECT 'master:warehouse:list',             'master:warehouse'
        UNION ALL SELECT 'master:warehouse:get',              'master:warehouse'
        UNION ALL SELECT 'master:warehouse:update',           'master:warehouse'
        UNION ALL SELECT 'master:warehouse:status',           'master:warehouse'
        UNION ALL SELECT 'factory:create',                    'factory'
        UNION ALL SELECT 'factory:list',                      'factory'
        UNION ALL SELECT 'factory:get',                       'factory'
        UNION ALL SELECT 'factory:update',                    'factory'
        UNION ALL SELECT 'factory:status:update',             'factory'
        UNION ALL SELECT 'factory:export',                    'factory'
        UNION ALL SELECT 'purchase:demand:create',            'purchase:demand'
        UNION ALL SELECT 'purchase:demand:list',              'purchase:demand'
        UNION ALL SELECT 'purchase:demand:get',               'purchase:demand'
        UNION ALL SELECT 'purchase:demand:approve',           'purchase:demand'
        UNION ALL SELECT 'purchase:demand:close',             'purchase:demand'
        UNION ALL SELECT 'purchase:order:create',             'purchase:order'
        UNION ALL SELECT 'purchase:order:list',               'purchase:order'
        UNION ALL SELECT 'purchase:order:get',                'purchase:order'
        UNION ALL SELECT 'purchase:order:update',             'purchase:order'
        UNION ALL SELECT 'purchase:order:approve',            'purchase:order'
        UNION ALL SELECT 'purchase:order:ship',               'purchase:order'
        UNION ALL SELECT 'purchase:order:receive',            'purchase:order'
        UNION ALL SELECT 'purchase:order:close',              'purchase:order'
        UNION ALL SELECT 'purchase:in:stock:list',            'purchase:in:stock'
        UNION ALL SELECT 'purchase:in:stock:approve',         'purchase:in:stock'
        UNION ALL SELECT 'purchase:in:stock:upload',          'purchase:in:stock'
        UNION ALL SELECT 'sales:order:create',                'sales:order'
        UNION ALL SELECT 'sales:order:list',                  'sales:order'
        UNION ALL SELECT 'sales:order:get',                   'sales:order'
        UNION ALL SELECT 'sales:order:update',                'sales:order'
        UNION ALL SELECT 'sales:order:delete',                'sales:order'
        UNION ALL SELECT 'sales:order:confirm',               'sales:order'
        UNION ALL SELECT 'sales:order:cancel',                'sales:order'
        UNION ALL SELECT 'sales:delivery:create',             'sales:delivery'
        UNION ALL SELECT 'sales:delivery:list',               'sales:delivery'
        UNION ALL SELECT 'sales:delivery:get',                'sales:delivery'
        UNION ALL SELECT 'sales:delivery:confirm',            'sales:delivery'
        UNION ALL SELECT 'sales:delivery:complete',           'sales:delivery'
        UNION ALL SELECT 'sales:delivery:cancel',             'sales:delivery'
        UNION ALL SELECT 'production:bom:create',             'production:bom'
        UNION ALL SELECT 'production:bom:get',                'production:bom'
        UNION ALL SELECT 'production:bom:list',               'production:bom'
        UNION ALL SELECT 'production:bom:update',             'production:bom'
        UNION ALL SELECT 'production:bom:disable',            'production:bom'
        UNION ALL SELECT 'production:bom:explosion',          'production:bom'
        UNION ALL SELECT 'production:bom:requirement',        'production:bom'
        UNION ALL SELECT 'production:demand:list',            'production:demand'
        UNION ALL SELECT 'production:demand:get',             'production:demand'
        UNION ALL SELECT 'production:order:create',           'production:order'
        UNION ALL SELECT 'production:order:list',             'production:order'
        UNION ALL SELECT 'production:order:get',              'production:order'
        UNION ALL SELECT 'production:order:release',          'production:order'
        UNION ALL SELECT 'production:order:start',            'production:order'
        UNION ALL SELECT 'production:order:complete',         'production:order'
        UNION ALL SELECT 'production:order:cancel',           'production:order'
        UNION ALL SELECT 'inventory:material-stock:create',   'inventory:material-stock'
        UNION ALL SELECT 'inventory:material-stock:list',     'inventory:material-stock'
        UNION ALL SELECT 'inventory:material-stock:get',      'inventory:material-stock'
        UNION ALL SELECT 'inventory:transaction:list',        'inventory:transaction'
        UNION ALL SELECT 'inventory:transaction:export',      'inventory:transaction'
        UNION ALL SELECT 'inventory:transaction:import',      'inventory:transaction'
    ) m ON m.code = p.code
    JOIN sys_permission par ON par.code = m.parent_code
SET p.parent_id = par.id;

-- -----------------------------------------------------------------------------
-- 3. 超级管理员角色（若已存在则更新，保证 role_id = 1 可用）
-- -----------------------------------------------------------------------------
INSERT INTO sys_role (id, name, code, sort, status, create_time, update_time, deleted)
VALUES (1, '超级管理员', 'admin', 100, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

-- -----------------------------------------------------------------------------
-- 4. 全部权限授予超级管理员（先清后插，幂等）
-- -----------------------------------------------------------------------------
DELETE FROM sys_role_permission WHERE role_id = 1;

SET @rp_max_id = (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT @rp_max_id + ROW_NUMBER() OVER (ORDER BY id), 1, id
FROM sys_permission
WHERE deleted = 0;

-- -----------------------------------------------------------------------------
-- 校验
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS total_permission FROM sys_permission WHERE deleted = 0;
SELECT COUNT(*) AS total_bind FROM sys_role_permission WHERE role_id = 1;
SELECT p.id, p.name, p.code, p.type, p.parent_id
FROM sys_permission p
WHERE p.deleted = 0
ORDER BY p.id;
