-- 前端动态路由必须有菜单；visible=1 才会被 MenuServiceImpl 返回。
-- 预留 81000..81999 菜单 ID；冲突时报错，不覆盖已有记录。
USE `smart-erp`;
SET NAMES utf8mb4;
START TRANSACTION;
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
COMMIT;
