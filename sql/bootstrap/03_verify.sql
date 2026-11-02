-- 开发 seed 验收：任何条件失败会触发 CHECK 约束错误，mysql 返回非零。
USE `smart-erp`;
CREATE TEMPORARY TABLE seed_assertion (
    description VARCHAR(100), ok TINYINT NOT NULL, CHECK (ok = 1)
);
INSERT INTO seed_assertion SELECT 'active admin, role and department', COUNT(*) = 1
FROM sys_user u JOIN sys_dept d ON d.id=u.dept_id
JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
WHERE u.username='admin' AND u.status=1 AND u.deleted=0
AND d.status=1 AND d.deleted=0 AND r.code='admin' AND r.status=1 AND r.deleted=0;
INSERT INTO seed_assertion SELECT 'BCrypt password format', COUNT(*) = 1
FROM sys_user WHERE username='admin' AND password REGEXP '^\\$2[aby]\\$[0-9]{2}\\$.{53}$';
INSERT INTO seed_assertion SELECT 'permission count', COUNT(*) >= 117
FROM sys_permission WHERE status=1 AND deleted=0;
INSERT INTO seed_assertion SELECT 'visible menu count', COUNT(*) >= 37
FROM sys_menu WHERE visible=1 AND status=1 AND deleted=0;
INSERT INTO seed_assertion SELECT 'menu parent references', COUNT(*)=0
FROM sys_menu m LEFT JOIN sys_menu p ON p.id=m.parent_id
WHERE m.deleted=0 AND m.parent_id IS NOT NULL AND (p.id IS NULL OR p.deleted<>0);
INSERT INTO seed_assertion SELECT 'menu permission references', COUNT(*)=0
FROM sys_menu m LEFT JOIN sys_permission p ON p.code=m.permission_code AND p.deleted=0 AND p.status=1
WHERE m.deleted=0 AND m.permission_code IS NOT NULL AND p.id IS NULL;
INSERT INTO seed_assertion SELECT 'admin role permission completeness', COUNT(*)=0
FROM sys_role r CROSS JOIN sys_permission p
LEFT JOIN sys_role_permission rp ON rp.role_id=r.id AND rp.permission_id=p.id
WHERE r.code='admin' AND p.deleted=0 AND p.status=1 AND rp.id IS NULL;
INSERT INTO seed_assertion SELECT 'admin role menu completeness', COUNT(*)=0
FROM sys_role r CROSS JOIN sys_menu m
LEFT JOIN sys_role_menu rm ON rm.role_id=r.id AND rm.menu_id=m.id
WHERE r.code='admin' AND m.deleted=0 AND m.status=1 AND rm.id IS NULL;
SELECT description, ok FROM seed_assertion;
DROP TEMPORARY TABLE seed_assertion;
