-- Bind each physical schema to exactly one enterprise; the operator writes the row.
CREATE TABLE sys_tenant_identity (
 singleton TINYINT PRIMARY KEY,
 tenant_id VARCHAR(64) NOT NULL UNIQUE,
 CHECK (singleton=1)
);

-- Frozen public seed; no developer account or password.

-- 从 src/main/java 的 @PreAuthorize 提取精确权限码；变更鉴权时同步维护。
-- 仅新增缺失 code；保留已有权限的状态、名称和逻辑删除标记。
SET NAMES utf8mb4;
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '发布', 'eip:notification:publish', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:publish');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'eip:notification:recipient:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:recipient:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'eip:notification:template:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '删除', 'eip:notification:template:delete', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:delete');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'eip:notification:template:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'eip:notification:template:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'eip:notification:template:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'eip:notification:template:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'eip:notification:template:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'factory:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'factory:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'factory:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'factory:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'factory:status:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:status:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'factory:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'factory:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'inv:finish-warehousing:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inv:finish-warehousing:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'inv:finish-warehousing:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inv:finish-warehousing:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'inv:finish-warehousing:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inv:finish-warehousing:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'inventory:material-stock:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:material-stock:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'inventory:material-stock:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:material-stock:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'inventory:material-stock:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:material-stock:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'inventory:transaction:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transaction:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导入', 'inventory:transaction:import', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transaction:import');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'inventory:transaction:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transaction:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:customer:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'master:customer:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:customer:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:customer:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:customer:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:customer:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:customer:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:material-supplier:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material-supplier:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:material-supplier:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material-supplier:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:material-supplier:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material-supplier:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:material:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'master:material:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:material:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:material:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:material:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:material:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:material:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:production_line:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'master:production_line:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:production_line:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:production_line:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:production_line:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:production_line:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:production_line:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:supplier:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '导出', 'master:supplier:export', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:export');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:supplier:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:supplier:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:supplier:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:supplier:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:supplier:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:warehouse:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:warehouse:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:warehouse:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:warehouse:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:warehouse:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:warehouse:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:warehouse:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:warehouse:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:warehouse:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:warehouse:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'master:workshop:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:workshop:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'master:workshop:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:workshop:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'master:workshop:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:workshop:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '状态', 'master:workshop:status', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:workshop:status');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'master:workshop:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'master:workshop:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'prd:report:add', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:add');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '审核', 'prd:report:approve', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:approve');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '取消', 'prd:report:cancel', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:cancel');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '完成', 'prd:report:finish', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:finish');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'prd:report:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'prd:report:page', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:page');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '驳回', 'prd:report:reject', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'prd:report:reject');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'production:bom:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '停用', 'production:bom:disable', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:disable');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '展开', 'production:bom:explosion', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:explosion');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'production:bom:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'production:bom:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '需求', 'production:bom:requirement', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:requirement');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'production:bom:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:bom:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'production:demand:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:demand:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'production:demand:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:demand:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '取消', 'production:order:cancel', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:cancel');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '完成', 'production:order:complete', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:complete');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'production:order:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'production:order:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'production:order:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '下达', 'production:order:release', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:release');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '开工', 'production:order:start', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:order:start');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '审核', 'production:picking:approve', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:picking:approve');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '确认', 'production:picking:confirm', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:picking:confirm');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'production:picking:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:picking:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'production:picking:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'production:picking:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '审核', 'purchase:demand:approve', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:demand:approve');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '关闭', 'purchase:demand:close', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:demand:close');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'purchase:demand:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:demand:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'purchase:demand:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:demand:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'purchase:demand:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:demand:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '审核', 'purchase:in:stock:approve', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:in:stock:approve');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'purchase:in:stock:detail', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:in:stock:detail');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'purchase:in:stock:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:in:stock:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '上架', 'purchase:in:stock:upload', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:in:stock:upload');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '审核', 'purchase:order:approve', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:approve');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '关闭', 'purchase:order:close', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:close');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'purchase:order:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'purchase:order:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'purchase:order:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT 'receive', 'purchase:order:receive', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:receive');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT 'ship', 'purchase:order:ship', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:ship');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'purchase:order:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:order:update');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '取消', 'sales:delivery:cancel', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:cancel');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '完成', 'sales:delivery:complete', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:complete');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '确认', 'sales:delivery:confirm', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:confirm');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'sales:delivery:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'sales:delivery:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'sales:delivery:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:delivery:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '取消', 'sales:order:cancel', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:cancel');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '确认', 'sales:order:confirm', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:confirm');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '新增', 'sales:order:create', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:create');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '删除', 'sales:order:delete', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:delete');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '详情', 'sales:order:get', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:get');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '列表', 'sales:order:list', 1, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:list');
INSERT INTO sys_permission (name, code, type, status, sort, create_time, deleted)
SELECT '修改', 'sales:order:update', 2, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'sales:order:update');

-- 前端动态路由必须有菜单；visible=1 才会被 MenuServiceImpl 返回。
-- 预留 81000..81999 菜单 ID；冲突时报错，不覆盖已有记录。
SET NAMES utf8mb4;
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81000, 'seed-system', '/system', '', '系统管理', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system' OR name = 'seed-system');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81001, 'seed-master', '/master', '', '基础资料', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master' OR name = 'seed-master');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81002, 'seed-sales', '/sales', '', '销售管理', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/sales' OR name = 'seed-sales');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81003, 'seed-inventory', '/inventory', '', '库存管理', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/inventory' OR name = 'seed-inventory');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81004, 'seed-product', '/product', '', '生产管理', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product' OR name = 'seed-product');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81005, 'seed-purchase', '/purchase', '', '采购管理', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/purchase' OR name = 'seed-purchase');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81006, 'seed-eip', '/eip', '', '协同办公', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/eip' OR name = 'seed-eip');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81100, 'seed-system-user', '/system/user', 'system/user/index', '用户管理', (SELECT id FROM sys_menu WHERE path = '/system'), NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/user' OR name = 'seed-system-user');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81101, 'seed-system-role', '/system/role', 'system/role/index', '角色管理', (SELECT id FROM sys_menu WHERE path = '/system'), NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/role' OR name = 'seed-system-role');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81102, 'seed-system-dept', '/system/dept', 'system/dept/index', '部门管理', (SELECT id FROM sys_menu WHERE path = '/system'), NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/dept' OR name = 'seed-system-dept');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81103, 'seed-system-menu', '/system/menu', 'system/menu/index', '菜单管理', (SELECT id FROM sys_menu WHERE path = '/system'), NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/menu' OR name = 'seed-system-menu');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81104, 'seed-system-permission', '/system/permission', 'system/permission/index', '权限管理', (SELECT id FROM sys_menu WHERE path = '/system'), NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/permission' OR name = 'seed-system-permission');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81105, 'seed-master-customer', '/master/customer', 'master/customer/index', '客户', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:customer:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/customer' OR name = 'seed-master-customer');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81106, 'seed-master-supplier', '/master/supplier', 'master/supplier/index', '供应商', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:supplier:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/supplier' OR name = 'seed-master-supplier');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81107, 'seed-master-material', '/master/material', 'master/material/index', '物料', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:material:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/material' OR name = 'seed-master-material');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81108, 'seed-master-material-supplier', '/master/material-supplier', 'master/material-supplier/index', '物料供应商', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:material-supplier:create', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/material-supplier' OR name = 'seed-master-material-supplier');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81109, 'seed-master-warehouse', '/master/warehouse', 'master/warehouse/index', '仓库', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:warehouse:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/warehouse' OR name = 'seed-master-warehouse');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81110, 'seed-master-factory', '/master/factory', 'master/factory/index', '工厂', (SELECT id FROM sys_menu WHERE path = '/master'), 'factory:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/factory' OR name = 'seed-master-factory');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81111, 'seed-master-workshop', '/master/workshop', 'master/workshop/index', '车间', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:workshop:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/workshop' OR name = 'seed-master-workshop');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81112, 'seed-master-production_line', '/master/production_line', 'master/production_line/index', '产线', (SELECT id FROM sys_menu WHERE path = '/master'), 'master:production_line:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/master/production_line' OR name = 'seed-master-production_line');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81113, 'seed-sales-sales_order', '/sales/sales_order', 'sales/sales_order/index', '销售订单', (SELECT id FROM sys_menu WHERE path = '/sales'), 'sales:order:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/sales/sales_order' OR name = 'seed-sales-sales_order');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81114, 'seed-sales-sales_delivery', '/sales/sales_delivery', 'sales/sales_delivery/index', '销售出库', (SELECT id FROM sys_menu WHERE path = '/sales'), 'sales:delivery:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/sales/sales_delivery' OR name = 'seed-sales-sales_delivery');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81115, 'seed-inventory-material-stock', '/inventory/material-stock', 'inventory/material-stock/index', '物料库存', (SELECT id FROM sys_menu WHERE path = '/inventory'), 'inventory:material-stock:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/inventory/material-stock' OR name = 'seed-inventory-material-stock');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81116, 'seed-inventory-transaction', '/inventory/transaction', 'inventory/transaction/index', '库存流水', (SELECT id FROM sys_menu WHERE path = '/inventory'), 'inventory:transaction:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/inventory/transaction' OR name = 'seed-inventory-transaction');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81117, 'seed-inventory-finish_warehousing', '/inventory/finish_warehousing', 'inventory/finish_warehousing/index', '完工入库', (SELECT id FROM sys_menu WHERE path = '/inventory'), 'inv:finish-warehousing:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/inventory/finish_warehousing' OR name = 'seed-inventory-finish_warehousing');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81118, 'seed-product-bom-bom_management', '/product/bom/bom_management', 'product/bom/bom_management/index', 'BOM管理', (SELECT id FROM sys_menu WHERE path = '/product'), 'production:bom:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/bom/bom_management' OR name = 'seed-product-bom-bom_management');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81119, 'seed-product-bom-bom_tree_query', '/product/bom/bom_tree_query', 'product/bom/bom_tree_query/index', 'BOM树查询', (SELECT id FROM sys_menu WHERE path = '/product'), 'production:bom:explosion', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/bom/bom_tree_query' OR name = 'seed-product-bom-bom_tree_query');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81120, 'seed-product-production_demand', '/product/production_demand', 'product/production_demand/index', '生产需求', (SELECT id FROM sys_menu WHERE path = '/product'), 'production:demand:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/production_demand' OR name = 'seed-product-production_demand');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81121, 'seed-product-production_order', '/product/production_order', 'product/production_order/index', '生产订单', (SELECT id FROM sys_menu WHERE path = '/product'), 'production:order:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/production_order' OR name = 'seed-product-production_order');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81122, 'seed-product-production_picking', '/product/production_picking', 'product/production_picking/index', '生产领料', (SELECT id FROM sys_menu WHERE path = '/product'), 'production:picking:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/production_picking' OR name = 'seed-product-production_picking');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81123, 'seed-product-production_report', '/product/production_report', 'product/production_report/index', '生产报工', (SELECT id FROM sys_menu WHERE path = '/product'), 'prd:report:page', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/product/production_report' OR name = 'seed-product-production_report');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81124, 'seed-purchase-purchase_demand', '/purchase/purchase_demand', 'purchase/purchase_demand/index', '采购需求', (SELECT id FROM sys_menu WHERE path = '/purchase'), 'purchase:demand:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/purchase/purchase_demand' OR name = 'seed-purchase-purchase_demand');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81125, 'seed-purchase-purchase_order', '/purchase/purchase_order', 'purchase/purchase_order/index', '采购订单', (SELECT id FROM sys_menu WHERE path = '/purchase'), 'purchase:order:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/purchase/purchase_order' OR name = 'seed-purchase-purchase_order');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81126, 'seed-purchase-purchase_in_stock', '/purchase/purchase_in_stock', 'purchase/purchase_in_stock/index', '采购入库', (SELECT id FROM sys_menu WHERE path = '/purchase'), 'purchase:in:stock:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/purchase/purchase_in_stock' OR name = 'seed-purchase-purchase_in_stock');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81127, 'seed-eip-notification-publish', '/eip/notification/publish', 'eip/notification/publish/index', '发布通知', (SELECT id FROM sys_menu WHERE path = '/eip'), 'eip:notification:publish', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/eip/notification/publish' OR name = 'seed-eip-notification-publish');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81128, 'seed-eip-notification-template', '/eip/notification/template', 'eip/notification/template/index', '通知模板', (SELECT id FROM sys_menu WHERE path = '/eip'), 'eip:notification:template:list', 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/eip/notification/template' OR name = 'seed-eip-notification-template');
INSERT INTO sys_menu (id, name, path, component, title, parent_id, permission_code, visible, status, create_time, deleted)
SELECT 81900, 'seed-ai', '/ai', 'ai/index', 'AI助手', NULL, NULL, 1, 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/ai' OR name = 'seed-ai');


-- Existing active administrators receive new public permissions and menus.
INSERT INTO sys_role_permission (id,role_id,permission_id)
SELECT UUID_SHORT(),r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code='admin' AND r.status=1 AND r.deleted=0 AND p.status=1 AND p.deleted=0
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO sys_role_menu (id,role_id,menu_id)
SELECT UUID_SHORT(),r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code='admin' AND r.status=1 AND r.deleted=0 AND m.status=1 AND m.deleted=0
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id);
