-- 仅显式手工执行；应用启动和 Flyway 不会自动创建开发账号。
-- 默认 admin / OriginDev@123，BCrypt cost=10；首次登录后修改密码。
-- 保留已有同名账号/角色/部门及其密码、状态和删除标记，不负责修复旧库。
-- 必须先执行 permission_init.sql 和 menu_init.sql，停服并单线程运行。
USE `smart-erp`;
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO sys_dept (id, name, code, parent_id, status, sort, create_time, deleted)
SELECT 80001, '默认部门', 'DEFAULT', NULL, 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'DEFAULT');
-- RoleConverter 按 code=admin 判断超级管理员，不能改成 ROLE_ADMIN。
INSERT INTO sys_role (id, name, code, status, sort, create_time, deleted)
SELECT 80001, '超级管理员', 'admin', 1, 0, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'admin');
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, create_time, deleted)
SELECT 80001, 'admin', '$2a$10$05vm7.F0SapQ/Mu/T9V5/uzoA44aVQc005naloZdT7iIB.dc1hj7O',
       '开发管理员', (SELECT id FROM sys_dept WHERE code = 'DEFAULT'), 1, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');
-- 关联按自然键找到的真实 ID 创建；不依赖上面的预留 ID。
INSERT INTO sys_user_role (id, user_id, role_id)
SELECT UUID_SHORT(), u.id, r.id FROM sys_user u CROSS JOIN sys_role r
WHERE u.username = 'admin' AND r.code = 'admin' AND u.deleted = 0 AND r.deleted = 0
  AND u.status = 1 AND r.status = 1
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'admin' AND r.deleted = 0 AND r.status = 1 AND p.deleted = 0 AND p.status = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT UUID_SHORT(), r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code = 'admin' AND r.deleted = 0 AND r.status = 1 AND m.deleted = 0 AND m.status = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id);
COMMIT;
