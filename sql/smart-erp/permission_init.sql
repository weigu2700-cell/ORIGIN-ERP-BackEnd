-- 从 src/main/java 的 @PreAuthorize 提取精确权限码；变更鉴权时同步维护。
-- 仅新增缺失 code；保留已有权限的状态、名称和逻辑删除标记。
USE `smart-erp`;
SET NAMES utf8mb4;
START TRANSACTION;
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
COMMIT;
