-- ORIGIN ERP 演示数据
-- 用途：为本地开发环境填充系统、主数据、库存和销售业务的示例记录。
-- 执行前请先执行 sql/smart-erp 目录下的建表脚本，并确认当前数据库为 `smart-erp`。
-- 本文件使用 INSERT IGNORE，可重复执行；演示数据使用 900000000000xxx 范围的 ID。
-- 演示账号：admin，密码：123456。

USE `smart-erp`;

START TRANSACTION;

-- 当前仓库中未提供这三张表的 DDL，按后端实体补齐；已有表时不会覆盖。
CREATE TABLE IF NOT EXISTS md_material_supplier (
    id bigint NOT NULL PRIMARY KEY,
    material_id bigint NOT NULL,
    supplier_id bigint NOT NULL,
    material_supplier_code varchar(100) NOT NULL,
    purchase_price decimal(18,4) NOT NULL DEFAULT 0,
    lead_time_days int NOT NULL DEFAULT 0,
    preferred tinyint NOT NULL DEFAULT 0,
    min_order_qty decimal(18,4) NOT NULL DEFAULT 0,
    status tinyint NOT NULL DEFAULT 1,
    remark varchar(255) NULL,
    create_time datetime NOT NULL,
    update_time datetime NULL,
    deleted tinyint NOT NULL DEFAULT 0,
    UNIQUE KEY uk_material_supplier_code (material_supplier_code),
    UNIQUE KEY uk_material_supplier_pair (material_id, supplier_id)
) COMMENT='物料供应商关系';

CREATE TABLE IF NOT EXISTS sal_delivery (
    id bigint NOT NULL PRIMARY KEY,
    delivery_no varchar(100) NOT NULL,
    sales_order_id bigint NOT NULL,
    sales_order_no varchar(100) NOT NULL,
    customer_id bigint NOT NULL,
    delivery_date datetime NULL,
    status tinyint NOT NULL DEFAULT 0,
    remark varchar(255) NULL,
    create_time datetime NOT NULL,
    update_time datetime NULL,
    UNIQUE KEY uk_delivery_no (delivery_no)
) COMMENT='销售出库单';

CREATE TABLE IF NOT EXISTS sal_delivery_item (
    id bigint NOT NULL PRIMARY KEY,
    delivery_id bigint NOT NULL,
    line_no int NOT NULL,
    sales_order_item_id bigint NOT NULL,
    material_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    quantity decimal(18,4) NOT NULL
) COMMENT='销售出库明细';

-- 系统基础数据
INSERT IGNORE INTO sys_dept
    (id, name, code, parent_id, status, sort, create_time, update_time, deleted)
VALUES
    (900000000000001, '演示事业部', 'DEMO', 0, 1, 1, NOW(), NOW(), 0),
    (900000000000002, '销售中心', 'DEMO-SALES', 900000000000001, 1, 2, NOW(), NOW(), 0),
    (900000000000003, '供应链中心', 'DEMO-SUPPLY', 900000000000001, 1, 3, NOW(), NOW(), 0),
    (900000000000004, '生产中心', 'DEMO-PRODUCTION', 900000000000001, 1, 4, NOW(), NOW(), 0);

INSERT IGNORE INTO sys_role
    (id, name, code, sort, status, create_time, update_time, deleted)
VALUES
    (900000000000101, '演示管理员', 'admin', 1, 1, NOW(), NOW(), 0),
    (900000000000102, '销售专员', 'demo-sales', 2, 1, NOW(), NOW(), 0),
    (900000000000103, '仓库专员', 'demo-warehouse', 3, 1, NOW(), NOW(), 0);

-- 菜单 visible=0 表示显示，component 与前端 src/views 路径保持一致。
INSERT IGNORE INTO sys_menu
    (id, name, title, path, visible, component, icon, parent_id, status, create_time, update_time, deleted, permission_code)
VALUES
    (900000000000201, '工作台', '工作台', '/home', 0, 'home/home', 'House', 0, 1, NOW(), NOW(), 0, 'home:view'),
    (900000000000202, '主数据', '主数据', '/master', 0, 'Layout', 'Files', 0, 1, NOW(), NOW(), 0, 'master:view'),
    (900000000000203, '系统管理', '系统管理', '/system', 0, 'Layout', 'Setting', 0, 1, NOW(), NOW(), 0, 'system:view'),
    (900000000000204, '销售管理', '销售管理', '/sales', 0, 'Layout', 'ShoppingCart', 0, 1, NOW(), NOW(), 0, 'sales:view'),
    (900000000000205, '库存管理', '库存管理', '/inventory', 0, 'Layout', 'Goods', 0, 1, NOW(), NOW(), 0, 'inventory:view'),
    (900000000000211, '物料管理', '物料管理', '/master/material', 0, 'master/material/index', 'Box', 900000000000202, 1, NOW(), NOW(), 0, 'master:material:view'),
    (900000000000212, '供应商管理', '供应商管理', '/master/supplier', 0, 'master/supplier/index', 'Van', 900000000000202, 1, NOW(), NOW(), 0, 'master:supplier:view'),
    (900000000000213, '客户管理', '客户管理', '/master/customer', 0, 'master/customer/index', 'User', 900000000000202, 1, NOW(), NOW(), 0, 'master:customer:view'),
    (900000000000214, '工厂管理', '工厂管理', '/master/factory', 0, 'master/factory/index', 'OfficeBuilding', 900000000000202, 1, NOW(), NOW(), 0, 'master:factory:view'),
    (900000000000215, '仓库管理', '仓库管理', '/master/warehouse', 0, 'master/warehouse/index', 'Goods', 900000000000202, 1, NOW(), NOW(), 0, 'master:warehouse:view'),
    (900000000000216, '车间管理', '车间管理', '/master/workshop', 0, 'master/workshop/index', 'SetUp', 900000000000202, 1, NOW(), NOW(), 0, 'master:workshop:view'),
    (900000000000217, '产线管理', '产线管理', '/master/production-line', 0, 'master/production_line/index', 'Operation', 900000000000202, 1, NOW(), NOW(), 0, 'master:production-line:view'),
    (900000000000218, '物料供应关系', '物料供应关系', '/master/material-supplier', 0, 'master/material-supplier/index', 'Connection', 900000000000202, 1, NOW(), NOW(), 0, 'master:material-supplier:view'),
    (900000000000221, '订单管理', '订单管理', '/sales/order', 0, 'sales/salesOrder/index', 'Tickets', 900000000000204, 1, NOW(), NOW(), 0, 'sales:order:view'),
    (900000000000222, '出库管理', '出库管理', '/sales/delivery', 0, 'sales/salesDelivery/index', 'TakeawayBox', 900000000000204, 1, NOW(), NOW(), 0, 'sales:delivery:view'),
    (900000000000223, '库存现状', '库存现状', '/inventory/material-stock', 0, 'inventory/material-stock/index', 'Goods', 900000000000205, 1, NOW(), NOW(), 0, 'inventory:stock:view'),
    (900000000000224, '库存流水', '库存流水', '/inventory/transaction', 0, 'inventory/transaction/index', 'List', 900000000000205, 1, NOW(), NOW(), 0, 'inventory:transaction:view'),
    (900000000000231, '用户管理', '用户管理', '/system/user', 0, 'system/user/index', 'UserFilled', 900000000000203, 1, NOW(), NOW(), 0, 'system:user:view'),
    (900000000000232, '角色管理', '角色管理', '/system/role', 0, 'system/role/index', 'Avatar', 900000000000203, 1, NOW(), NOW(), 0, 'system:role:view'),
    (900000000000233, '菜单管理', '菜单管理', '/system/menu', 0, 'system/menu/index', 'Menu', 900000000000203, 1, NOW(), NOW(), 0, 'system:menu:view');

INSERT IGNORE INTO sys_permission
    (id, name, code, type, parent_id, status, sort, remark, create_time, update_time, deleted)
VALUES
    (900000000000301, '查看主数据', 'master:view', 1, NULL, 1, 1, '演示权限', NOW(), NOW(), 0),
    (900000000000302, '查看销售业务', 'sales:view', 1, NULL, 1, 2, '演示权限', NOW(), NOW(), 0),
    (900000000000303, '查看系统管理', 'system:view', 1, NULL, 1, 3, '演示权限', NOW(), NOW(), 0),
    (900000000000304, '查看库存业务', 'inventory:view', 1, NULL, 1, 4, '演示权限', NOW(), NOW(), 0),
    (900000000000311, '物料查询', 'master:material:view', 2, 900000000000301, 1, 1, NULL, NOW(), NOW(), 0),
    (900000000000312, '供应商查询', 'master:supplier:view', 2, 900000000000301, 1, 2, NULL, NOW(), NOW(), 0),
    (900000000000313, '客户查询', 'master:customer:view', 2, 900000000000301, 1, 3, NULL, NOW(), NOW(), 0),
    (900000000000314, '仓库查询', 'master:warehouse:view', 2, 900000000000301, 1, 4, NULL, NOW(), NOW(), 0),
    (900000000000321, '订单查询', 'sales:order:view', 2, 900000000000302, 1, 1, NULL, NOW(), NOW(), 0),
    (900000000000322, '出库查询', 'sales:delivery:view', 2, 900000000000302, 1, 2, NULL, NOW(), NOW(), 0),
    (900000000000323, '库存查询', 'inventory:stock:view', 2, 900000000000304, 1, 1, NULL, NOW(), NOW(), 0),
    (900000000000324, '库存流水查询', 'inventory:transaction:view', 2, 900000000000304, 1, 2, NULL, NOW(), NOW(), 0),
    (900000000000331, '用户查询', 'system:user:view', 2, 900000000000303, 1, 1, NULL, NOW(), NOW(), 0),
    (900000000000332, '角色查询', 'system:role:view', 2, 900000000000303, 1, 2, NULL, NOW(), NOW(), 0),
    (900000000000333, '菜单查询', 'system:menu:view', 2, 900000000000303, 1, 3, NULL, NOW(), NOW(), 0);

INSERT IGNORE INTO sys_user
    (id, username, password, phone, status, create_time, update_time, deleted, dept_id)
VALUES
    (900000000000401, 'admin', '$2a$10$2aW9eqG0B.PAEKLvrtfsHupDdnNouZyqLxDxlAoLp3PJ6NaM0CSa.', '13800000001', 1, NOW(), NOW(), 0, 900000000000001),
    (900000000000402, 'sales.demo', '$2a$10$2aW9eqG0B.PAEKLvrtfsHupDdnNouZyqLxDxlAoLp3PJ6NaM0CSa.', '13800000002', 1, NOW(), NOW(), 0, 900000000000002),
    (900000000000403, 'warehouse.demo', '$2a$10$2aW9eqG0B.PAEKLvrtfsHupDdnNouZyqLxDxlAoLp3PJ6NaM0CSa.', '13800000003', 1, NOW(), NOW(), 0, 900000000000003);

INSERT IGNORE INTO sys_user_role (id, user_id, role_id) VALUES
    (900000000000411, 900000000000401, 900000000000101),
    (900000000000412, 900000000000402, 900000000000102),
    (900000000000413, 900000000000403, 900000000000103);

INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id) VALUES
    (900000000000421, 900000000000101, 900000000000201),
    (900000000000422, 900000000000101, 900000000000202),
    (900000000000423, 900000000000101, 900000000000203),
    (900000000000424, 900000000000101, 900000000000204),
    (900000000000429, 900000000000101, 900000000000205),
    (900000000000425, 900000000000102, 900000000000201),
    (900000000000426, 900000000000102, 900000000000204),
    (900000000000427, 900000000000103, 900000000000201),
    (900000000000428, 900000000000103, 900000000000202),
    (900000000000430, 900000000000103, 900000000000205);

INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES
    (900000000000431, 900000000000101, 900000000000301),
    (900000000000432, 900000000000101, 900000000000302),
    (900000000000433, 900000000000101, 900000000000303),
    (900000000000436, 900000000000101, 900000000000304),
    (900000000000434, 900000000000102, 900000000000302),
    (900000000000435, 900000000000103, 900000000000301),
    (900000000000437, 900000000000103, 900000000000304);

-- 主数据
INSERT IGNORE INTO md_factory
    (id, name, short_name, code, address, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000501, '华东智能制造工厂', '华东工厂', 'FAC-DEMO-01', '江苏省苏州市工业园区', 1, '演示工厂', NOW(), NOW(), 0),
    (900000000000502, '华南装配工厂', '华南工厂', 'FAC-DEMO-02', '广东省东莞市松山湖', 1, '演示工厂', NOW(), NOW(), 0);

INSERT IGNORE INTO md_workshop
    (id, code, name, short_name, factory_id, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000511, 'WS-DEMO-01', '机加工车间', '机加工', 900000000000501, 1, NULL, NOW(), NOW(), 0),
    (900000000000512, 'WS-DEMO-02', '装配车间', '装配', 900000000000501, 1, NULL, NOW(), NOW(), 0),
    (900000000000513, 'WS-DEMO-03', '包装车间', '包装', 900000000000502, 1, NULL, NOW(), NOW(), 0);

INSERT IGNORE INTO md_production_line
    (id, name, code, workshop_id, capacity_per_day, remark, status, create_time, update_time, deleted)
VALUES
    (900000000000521, 'CNC 精加工线', 'LINE-DEMO-01', 900000000000511, 1200.00, '两班制', 1, NOW(), NOW(), 0),
    (900000000000522, '智能装配线', 'LINE-DEMO-02', 900000000000512, 800.00, '自动化线体', 1, NOW(), NOW(), 0),
    (900000000000523, '成品包装线', 'LINE-DEMO-03', 900000000000513, 1500.00, NULL, 1, NOW(), NOW(), 0);

-- 仓库类型在现有 DDL 中是唯一值，因此使用四种不同类型。
INSERT IGNORE INTO md_warehouse
    (id, name, code, type, factory_id, address, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000531, '原材料仓', 'WH-DEMO-MAT', 1, 900000000000501, '华东工厂 1 号库', 1, NULL, NOW(), NOW(), 0),
    (900000000000532, '半成品仓', 'WH-DEMO-SEMI', 2, 900000000000501, '华东工厂 2 号库', 1, NULL, NOW(), NOW(), 0),
    (900000000000533, '成品仓', 'WH-DEMO-FIN', 0, 900000000000502, '华南工厂成品库', 1, NULL, NOW(), NOW(), 0),
    (900000000000534, '不良品仓', 'WH-DEMO-SCRAP', 3, 900000000000502, '华南工厂隔离区', 1, NULL, NOW(), NOW(), 0);

INSERT IGNORE INTO md_material
    (id, code, name, type, spec, unit, safety_stock, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000541, 'MAT-DEMO-001', '铝合金外壳', 1, '6061-T6 / 220mm', '件', 200.0000, 1, '原材料示例', NOW(), NOW(), 0),
    (900000000000542, 'MAT-DEMO-002', '控制主板', 1, 'ORIGIN-MB-V2', '件', 100.0000, 1, '原材料示例', NOW(), NOW(), 0),
    (900000000000543, 'MAT-DEMO-003', '智能终端半成品', 2, 'ORIGIN-TERM-ASSY', '件', 50.0000, 1, '半成品示例', NOW(), NOW(), 0),
    (900000000000544, 'MAT-DEMO-004', '彩盒彩箱', 3, '420*280*180mm', '个', 300.0000, 1, '包装材料示例', NOW(), NOW(), 0),
    (900000000000545, 'MAT-DEMO-005', '导热硅脂', 4, '5W/mK / 100g', '支', 30.0000, 1, '耗材示例', NOW(), NOW(), 0);

INSERT IGNORE INTO md_supplier
    (id, code, name, short_name, contact_name, phone, email, address, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000551, 'SUP-DEMO-001', '苏州精密铝业有限公司', '苏州精密铝业', '李工', '13900000001', 'li@example.com', '江苏省苏州市', 1, NULL, NOW(), NOW(), 0),
    (900000000000552, 'SUP-DEMO-002', '深圳智造电子有限公司', '深圳智造电子', '陈工', '13900000002', 'chen@example.com', '广东省深圳市', 1, NULL, NOW(), NOW(), 0),
    (900000000000553, 'SUP-DEMO-003', '东莞包装科技有限公司', '东莞包装科技', '周经理', '13900000003', 'zhou@example.com', '广东省东莞市', 1, NULL, NOW(), NOW(), 0);

INSERT IGNORE INTO md_customer
    (id, code, name, short_name, contact_name, phone, email, address, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000561, 'CUS-DEMO-001', '华东智能设备有限公司', '华东智能', '王经理', '13700000001', 'wang@example.com', '上海市浦东新区', 1, '重点客户', NOW(), NOW(), 0),
    (900000000000562, 'CUS-DEMO-002', '华南工业自动化有限公司', '华南自动化', '赵经理', '13700000002', 'zhao@example.com', '广东省广州市', 1, NULL, NOW(), NOW(), 0),
    (900000000000563, 'CUS-DEMO-003', '北方新能源科技有限公司', '北方新能源', '刘经理', '13700000003', 'liu@example.com', '北京市朝阳区', 1, NULL, NOW(), NOW(), 0);

INSERT IGNORE INTO md_material_supplier
    (id, material_id, supplier_id, material_supplier_code, purchase_price, lead_time_days, preferred, min_order_qty, status, remark, create_time, update_time, deleted)
VALUES
    (900000000000571, 900000000000541, 900000000000551, 'MS-DEMO-001', 38.50, 7, 1, 100.0000, 1, '主供', NOW(), NOW(), 0),
    (900000000000572, 900000000000542, 900000000000552, 'MS-DEMO-002', 126.00, 10, 1, 50.0000, 1, '主供', NOW(), NOW(), 0),
    (900000000000573, 900000000000544, 900000000000553, 'MS-DEMO-003', 4.80, 5, 1, 200.0000, 1, '主供', NOW(), NOW(), 0),
    (900000000000574, 900000000000541, 900000000000552, 'MS-DEMO-004', 40.00, 12, 0, 100.0000, 1, '备选供应商', NOW(), NOW(), 0);

-- 库存现状与流水
INSERT IGNORE INTO inv_material_stock
    (id, warehouse_id, material_id, on_hand, reserved, version, create_time, update_time)
VALUES
    (900000000000601, 900000000000531, 900000000000541, 860.0000, 120.0000, 0, NOW(), NOW()),
    (900000000000602, 900000000000531, 900000000000542, 420.0000, 80.0000, 0, NOW(), NOW()),
    (900000000000603, 900000000000532, 900000000000543, 260.0000, 30.0000, 0, NOW(), NOW()),
    (900000000000604, 900000000000531, 900000000000544, 1250.0000, 100.0000, 0, NOW(), NOW()),
    (900000000000605, 900000000000533, 900000000000543, 180.0000, 20.0000, 0, NOW(), NOW());

-- 现有 inv_transaction.sql 将 business_type/business_no 定义为 tinyint，故这里填入可兼容旧表的数字。
INSERT IGNORE INTO inv_transaction
    (id, warehouse_id, material_id, transaction_type, business_type, business_no, quantity, before_on_hand, after_on_hand, before_reserved, after_reserved, remark, create_time)
VALUES
    (900000000000611, 900000000000531, 900000000000541, 1, 1, 1, 500.0000, 360.0000, 860.0000, 0.0000, 0.0000, '采购入库演示', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (900000000000612, 900000000000531, 900000000000541, 2, 2, 1, 120.0000, 860.0000, 860.0000, 0.0000, 120.0000, '销售订单预占演示', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (900000000000613, 900000000000532, 900000000000543, 1, 1, 2, 260.0000, 0.0000, 260.0000, 0.0000, 0.0000, '生产入库演示', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 销售订单及明细
INSERT IGNORE INTO sal_order
    (id, order_no, customer_id, status, order_date, delivery_date, total_amount, remark, create_time, update_time)
VALUES
    (900000000000701, 'SO-DEMO-20260901-001', 900000000000561, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 38500.00, '首批设备订单', NOW(), NOW()),
    (900000000000702, 'SO-DEMO-20260902-002', 900000000000562, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY), 19200.00, '待确认订单', NOW(), NOW()),
    (900000000000703, 'SO-DEMO-20260820-003', 900000000000563, 2, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 12600.00, '已完成订单', NOW(), NOW());

INSERT IGNORE INTO sal_order_item
    (id, sales_order_id, line_no, material_id, quantity, unit_price, amount, warehouse_id, delivery_date, create_time)
VALUES
    (900000000000711, 900000000000701, 1, 900000000000543, 100.0000, 320.0000, 32000.0000, 900000000000533, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),
    (900000000000712, 900000000000701, 2, 900000000000544, 500.0000, 13.0000, 6500.0000, 900000000000531, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),
    (900000000000713, 900000000000702, 1, 900000000000543, 60.0000, 320.0000, 19200.0000, 900000000000533, DATE_ADD(NOW(), INTERVAL 12 DAY), NOW()),
    (900000000000714, 900000000000703, 1, 900000000000543, 30.0000, 320.0000, 9600.0000, 900000000000533, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
    (900000000000715, 900000000000703, 2, 900000000000544, 300.0000, 10.0000, 3000.0000, 900000000000531, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- 销售出库及明细
INSERT IGNORE INTO sal_delivery
    (id, delivery_no, sales_order_id, sales_order_no, customer_id, delivery_date, status, remark, create_time, update_time)
VALUES
    (900000000000721, 'SD-DEMO-20260829-001', 900000000000703, 'SO-DEMO-20260820-003', 900000000000563, DATE_SUB(NOW(), INTERVAL 2 DAY), 3, '已完成出库', NOW(), NOW()),
    (900000000000722, 'SD-DEMO-20260903-002', 900000000000701, 'SO-DEMO-20260901-001', 900000000000561, DATE_ADD(NOW(), INTERVAL 5 DAY), 1, '已确认待出库', NOW(), NOW());

INSERT IGNORE INTO sal_delivery_item
    (id, delivery_id, line_no, sales_order_item_id, material_id, warehouse_id, quantity)
VALUES
    (900000000000731, 900000000000721, 1, 900000000000714, 900000000000543, 900000000000533, 30.0000),
    (900000000000732, 900000000000721, 2, 900000000000715, 900000000000544, 900000000000531, 300.0000),
    (900000000000733, 900000000000722, 1, 900000000000711, 900000000000543, 900000000000533, 100.0000),
    (900000000000734, 900000000000722, 2, 900000000000712, 900000000000544, 900000000000531, 500.0000);

COMMIT;
