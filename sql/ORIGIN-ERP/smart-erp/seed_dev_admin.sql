-- =============================================================================
-- 本地开发 RBAC 初始化脚本（可重复执行）
--
-- 解决两个问题：
--   1. 登不进去：sys_user.password 必须存 BCrypt 哈希。用 SQL 直接插入明文密码时，
--      LoginServiceImpl 里的 passwordEncoder.matches() 永远返回 false，接口报“密码错误”。
--   2. 登进去也用不了：用户必须至少有一个角色，角色必须绑定 sys_permission，
--      否则带 @PreAuthorize 的业务接口全部 403；一个角色都没有时
--      JwtAuthenticationFilter 还会因为权限查询异常而清空登录态。
--
-- 执行后得到：
--   * 角色 admin（超级管理员；MenuServiceImpl.isSuperAdmin 按 code='admin' 判定）
--   * 与后端 @PreAuthorize 编码一一对应的全部操作权限，并全部授予 admin 角色
--   * 账号 admin / admin123，已绑定 admin 角色
--   * 历史明文密码统一重置为 admin123；没有任何角色的账号补绑 admin 角色
--
-- 执行方式（Windows 示例）：
--   mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 smart-erp ^
--         -e "source D:/ORIGIN-ERP/ORIGIN-ERP-BackEnd/sql/ORIGIN-ERP/smart-erp/seed_dev_admin.sql"
--
-- 注意：第 6、7 步是给本地开发兜底用的，生产环境请删掉，
--       正式建号请走 POST /system/user/create（服务端会自动 BCrypt 加密）。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 管理员角色
-- -----------------------------------------------------------------------------
INSERT INTO sys_role (id, name, code, sort, status, create_time, update_time, deleted)
VALUES (1, '超级管理员', 'admin', 100, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

-- -----------------------------------------------------------------------------
-- 2. 操作权限（编码与后端 @PreAuthorize 保持一致，新增接口后请同步补充）
-- -----------------------------------------------------------------------------
INSERT INTO sys_permission (name, code, type, parent_id, status, sort, create_time, deleted)
VALUES
    ('工厂-新增', 'factory:create', 2, NULL, 1, 0, NOW(), 0),
    ('工厂-详情', 'factory:get', 2, NULL, 1, 0, NOW(), 0),
    ('工厂-列表', 'factory:list', 2, NULL, 1, 0, NOW(), 0),
    ('工厂-状态变更', 'factory:status:update', 2, NULL, 1, 0, NOW(), 0),
    ('工厂-修改', 'factory:update', 2, NULL, 1, 0, NOW(), 0),
    ('物料库存-新增', 'inventory:material-stock:create', 2, NULL, 1, 1, NOW(), 0),
    ('物料库存-详情', 'inventory:material-stock:get', 2, NULL, 1, 1, NOW(), 0),
    ('物料库存-列表', 'inventory:material-stock:list', 2, NULL, 1, 1, NOW(), 0),
    ('库存流水-导出', 'inventory:transaction:export', 2, NULL, 1, 2, NOW(), 0),
    ('库存流水-导入', 'inventory:transaction:import', 2, NULL, 1, 2, NOW(), 0),
    ('库存流水-列表', 'inventory:transaction:list', 2, NULL, 1, 2, NOW(), 0),
    ('客户-新增', 'master:customer:create', 2, NULL, 1, 3, NOW(), 0),
    ('客户-详情', 'master:customer:get', 2, NULL, 1, 3, NOW(), 0),
    ('客户-列表', 'master:customer:list', 2, NULL, 1, 3, NOW(), 0),
    ('客户-状态变更', 'master:customer:status', 2, NULL, 1, 3, NOW(), 0),
    ('客户-修改', 'master:customer:update', 2, NULL, 1, 3, NOW(), 0),
    ('物料供应商-新增', 'master:material-supplier:create', 2, NULL, 1, 4, NOW(), 0),
    ('物料供应商-状态变更', 'master:material-supplier:status', 2, NULL, 1, 4, NOW(), 0),
    ('物料供应商-修改', 'master:material-supplier:update', 2, NULL, 1, 4, NOW(), 0),
    ('物料-新增', 'master:material:create', 2, NULL, 1, 5, NOW(), 0),
    ('物料-详情', 'master:material:get', 2, NULL, 1, 5, NOW(), 0),
    ('物料-列表', 'master:material:list', 2, NULL, 1, 5, NOW(), 0),
    ('物料-状态变更', 'master:material:status', 2, NULL, 1, 5, NOW(), 0),
    ('物料-修改', 'master:material:update', 2, NULL, 1, 5, NOW(), 0),
    ('生产线-新增', 'master:production_line:create', 2, NULL, 1, 6, NOW(), 0),
    ('生产线-详情', 'master:production_line:get', 2, NULL, 1, 6, NOW(), 0),
    ('生产线-列表', 'master:production_line:list', 2, NULL, 1, 6, NOW(), 0),
    ('生产线-状态变更', 'master:production_line:status', 2, NULL, 1, 6, NOW(), 0),
    ('生产线-修改', 'master:production_line:update', 2, NULL, 1, 6, NOW(), 0),
    ('供应商-新增', 'master:supplier:create', 2, NULL, 1, 7, NOW(), 0),
    ('供应商-详情', 'master:supplier:get', 2, NULL, 1, 7, NOW(), 0),
    ('供应商-列表', 'master:supplier:list', 2, NULL, 1, 7, NOW(), 0),
    ('供应商-状态变更', 'master:supplier:status', 2, NULL, 1, 7, NOW(), 0),
    ('供应商-修改', 'master:supplier:update', 2, NULL, 1, 7, NOW(), 0),
    ('仓库-新增', 'master:warehouse:create', 2, NULL, 1, 8, NOW(), 0),
    ('仓库-详情', 'master:warehouse:get', 2, NULL, 1, 8, NOW(), 0),
    ('仓库-列表', 'master:warehouse:list', 2, NULL, 1, 8, NOW(), 0),
    ('仓库-状态变更', 'master:warehouse:status', 2, NULL, 1, 8, NOW(), 0),
    ('仓库-修改', 'master:warehouse:update', 2, NULL, 1, 8, NOW(), 0),
    ('BOM-新增', 'production:bom:create', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-禁用', 'production:bom:disable', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-展开', 'production:bom:explosion', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-详情', 'production:bom:get', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-列表', 'production:bom:list', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-需求计算', 'production:bom:requirement', 2, NULL, 1, 9, NOW(), 0),
    ('BOM-修改', 'production:bom:update', 2, NULL, 1, 9, NOW(), 0),
    ('生产需求-详情', 'production:demand:get', 2, NULL, 1, 10, NOW(), 0),
    ('生产需求-列表', 'production:demand:list', 2, NULL, 1, 10, NOW(), 0),
    ('生产订单-取消', 'production:order:cancel', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-完成', 'production:order:complete', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-新增', 'production:order:create', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-详情', 'production:order:get', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-列表', 'production:order:list', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-下达', 'production:order:release', 2, NULL, 1, 11, NOW(), 0),
    ('生产订单-开工', 'production:order:start', 2, NULL, 1, 11, NOW(), 0),
    ('采购需求-审批', 'purchase:demand:approve', 2, NULL, 1, 12, NOW(), 0),
    ('采购需求-关闭', 'purchase:demand:close', 2, NULL, 1, 12, NOW(), 0),
    ('采购需求-新增', 'purchase:demand:create', 2, NULL, 1, 12, NOW(), 0),
    ('采购需求-详情', 'purchase:demand:get', 2, NULL, 1, 12, NOW(), 0),
    ('采购需求-列表', 'purchase:demand:list', 2, NULL, 1, 12, NOW(), 0),
    ('采购订单-审批', 'purchase:order:approve', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-关闭', 'purchase:order:close', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-新增', 'purchase:order:create', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-详情', 'purchase:order:get', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-列表', 'purchase:order:list', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-收货', 'purchase:order:receive', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-发货', 'purchase:order:ship', 2, NULL, 1, 13, NOW(), 0),
    ('采购订单-修改', 'purchase:order:update', 2, NULL, 1, 13, NOW(), 0),
    ('销售出库-取消', 'sales:delivery:cancel', 2, NULL, 1, 14, NOW(), 0),
    ('销售出库-完成', 'sales:delivery:complete', 2, NULL, 1, 14, NOW(), 0),
    ('销售出库-确认', 'sales:delivery:confirm', 2, NULL, 1, 14, NOW(), 0),
    ('销售出库-新增', 'sales:delivery:create', 2, NULL, 1, 14, NOW(), 0),
    ('销售出库-详情', 'sales:delivery:get', 2, NULL, 1, 14, NOW(), 0),
    ('销售出库-列表', 'sales:delivery:list', 2, NULL, 1, 14, NOW(), 0),
    ('销售订单-取消', 'sales:order:cancel', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-确认', 'sales:order:confirm', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-新增', 'sales:order:create', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-删除', 'sales:order:delete', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-详情', 'sales:order:get', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-列表', 'sales:order:list', 2, NULL, 1, 15, NOW(), 0),
    ('销售订单-修改', 'sales:order:update', 2, NULL, 1, 15, NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

-- -----------------------------------------------------------------------------
-- 3. 管理员角色拥有全部权限（先清后插，保证幂等）
-- -----------------------------------------------------------------------------
DELETE FROM sys_role_permission WHERE role_id = 1;

SET @rp_max_id = (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT @rp_max_id + ROW_NUMBER() OVER (ORDER BY id), 1, id
FROM sys_permission
WHERE deleted = 0;

-- -----------------------------------------------------------------------------
-- 4. 管理员账号 admin / admin123（password 是 admin123 的 BCrypt 哈希）
-- -----------------------------------------------------------------------------
INSERT INTO sys_user (id, username, real_name, password, phone, status, create_time, update_time, deleted)
VALUES (1001, 'admin', '系统管理员', '$2a$10$vE.7wIpUPM3I9K2s.PMa7.pMua3REiLPRd1xA28KwLOD6wS1YQcKC', NULL, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), password = VALUES(password), status = 1, deleted = 0;

-- -----------------------------------------------------------------------------
-- 5. admin 账号绑定管理员角色
-- -----------------------------------------------------------------------------
DELETE FROM sys_user_role WHERE user_id = 1001 AND role_id = 1;

SET @ur_max_id = (SELECT COALESCE(MAX(id), 0) FROM sys_user_role);

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (@ur_max_id + 1, 1001, 1);

-- -----------------------------------------------------------------------------
-- 6. 修复历史明文密码：不是 BCrypt 哈希的一律重置为 admin123
--    （BCrypt 哈希形如 $2a$10$xxxx，明文永远匹配不上）
-- -----------------------------------------------------------------------------
UPDATE sys_user
SET password   = '$2a$10$vE.7wIpUPM3I9K2s.PMa7.pMua3REiLPRd1xA28KwLOD6wS1YQcKC',
    update_time = NOW()
WHERE deleted = 0
  AND password NOT LIKE '$2a$%'
  AND password NOT LIKE '$2b$%'
  AND password NOT LIKE '$2y$%';

-- -----------------------------------------------------------------------------
-- 7. 给还没有任何角色的账号补绑管理员角色
--    （用临时表，避免 INSERT ... SELECT 里引用目标表）
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_role_less_user;

CREATE TEMPORARY TABLE tmp_role_less_user AS
SELECT u.id
FROM sys_user u
         LEFT JOIN sys_user_role ur ON ur.user_id = u.id
WHERE u.deleted = 0
  AND ur.id IS NULL;

SET @ur_max_id2 = (SELECT COALESCE(MAX(id), 0) FROM sys_user_role);

INSERT INTO sys_user_role (id, user_id, role_id)
SELECT @ur_max_id2 + ROW_NUMBER() OVER (ORDER BY id), id, 1
FROM tmp_role_less_user;

DROP TEMPORARY TABLE IF EXISTS tmp_role_less_user;

-- -----------------------------------------------------------------------------
-- 8. 结果校验
-- -----------------------------------------------------------------------------
SELECT u.id, u.username, u.real_name, LEFT(u.password, 7) AS pwd_prefix, u.status,
       r.code AS role_code,
       (SELECT COUNT(*) FROM sys_role_permission rp WHERE rp.role_id = r.id) AS permission_count
FROM sys_user u
         LEFT JOIN sys_user_role ur ON ur.user_id = u.id
         LEFT JOIN sys_role r ON r.id = ur.role_id
WHERE u.deleted = 0
ORDER BY u.id;
