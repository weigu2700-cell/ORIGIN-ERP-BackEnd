package org.smart.erp.tenant.migration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/** 首位管理员由部署方提供 BCrypt；没有硬编码默认密码，也不重置已有密码。 */
public record TenantAdministrator(String username, String bcrypt) {

    public TenantAdministrator {
        if (username == null
                || !username.matches("[A-Za-z0-9_.@-]{1,50}")
                || bcrypt == null
                || !bcrypt.matches("\\$2[aby]\\$(1[0-6])\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("需要有效管理员账号和 cost=10..16 的 BCrypt 密码哈希");
        }
    }

    @Override
    public String toString() {
        return "TenantAdministrator[username=" + username + ", bcrypt=<redacted>]";
    }

    public void initialize(DataSource source) {
        var jdbc = new JdbcTemplate(source);
        new TransactionTemplate(new JdbcTransactionManager(source)).executeWithoutResult(tx -> {
            Integer users = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE username=?",
                    Integer.class,
                    username
            );
            if (users != null && users > 0) {
                Integer valid = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id
                        JOIN sys_role r ON r.id=ur.role_id JOIN sys_dept d ON d.id=u.dept_id
                        WHERE u.username=? AND u.status=1 AND u.deleted=0 AND r.code='admin'
                        AND r.status=1 AND r.deleted=0 AND d.status=1 AND d.deleted=0
                        """, Integer.class, username);
                if (valid == null || valid != 1) {
                    throw new IllegalStateException("已有账号不符合首位管理员约束，拒绝自动提权");
                }
                return;
            }

            jdbc.update("INSERT INTO sys_dept(id,name,code,status,sort,create_time,deleted)"
                    + " VALUES(80001,'默认部门','DEFAULT',1,0,NOW(),0)");
            jdbc.update("INSERT INTO sys_role(id,name,code,status,sort,create_time,deleted)"
                    + " VALUES(80001,'超级管理员','admin',1,0,NOW(),0)");
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,dept_id,status,create_time,deleted)"
                    + " VALUES(80001,?,?,'企业管理员',80001,1,NOW(),0)", username, bcrypt);
            jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(UUID_SHORT(),80001,80001)");
            jdbc.update("INSERT INTO sys_role_permission(id,role_id,permission_id)"
                    + " SELECT UUID_SHORT(),80001,id FROM sys_permission WHERE status=1 AND deleted=0");
            jdbc.update("INSERT INTO sys_role_menu(id,role_id,menu_id)"
                    + " SELECT UUID_SHORT(),80001,id FROM sys_menu WHERE status=1 AND deleted=0");
        });
    }

    public static void verifyExisting(DataSource source) {
        var passwords = new JdbcTemplate(source).queryForList("""
                SELECT DISTINCT u.password FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id
                JOIN sys_role r ON r.id=ur.role_id JOIN sys_dept d ON d.id=u.dept_id
                WHERE u.status=1 AND u.deleted=0 AND r.code='admin' AND r.status=1
                AND r.deleted=0 AND d.status=1 AND d.deleted=0
                """, String.class);
        if (passwords.stream().noneMatch(hash -> hash != null
                && hash.matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}"))) {
            throw new IllegalStateException("不存在可登录的有效管理员，请由部署方处理，工具不会创建通用开发账号");
        }
    }
}
