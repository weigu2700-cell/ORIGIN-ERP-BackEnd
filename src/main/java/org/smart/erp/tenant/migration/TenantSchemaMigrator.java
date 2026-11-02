package org.smart.erp.tenant.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 停服维护工具：平台事务和 MySQL DDL 不属于同一事务，按步骤记录并保留失败状态。 */
public final class TenantSchemaMigrator {

    public enum Action {
        NEW,
        ADOPT,
        UPGRADE
    }

    private final DataSource platform;

    public TenantSchemaMigrator(DataSource platform) {
        this.platform = platform;
    }

    public String execute(String tenantId, String connectionRef, DataSource target,
                          Action action, TenantAdministrator administrator) {
        if (tenantId == null
                || !tenantId.matches("[A-Za-z0-9_-]{1,64}")
                || connectionRef == null
                || action == null) {
            throw new IllegalArgumentException("需要明确的租户、连接引用和操作类型");
        }
        if (action == Action.NEW && administrator == null) {
            throw new IllegalArgumentException("新租户必须由部署方指定首位管理员");
        }

        var platformFlyway = TenantFlyway.platform(platform);
        requireCurrent(platformFlyway, "2");
        SchemaContract.verify(platform, "platform-v2");
        try (var connection = platform.getConnection()) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            String lock = "erp:tenant-migrate:" + tenantId;
            // 由同一平台连接持锁，跨进程串行；MySQL named lock 的名字最多 64 字节。
            lock = "erp:tm:" + UUID.nameUUIDFromBytes(lock.getBytes(StandardCharsets.UTF_8));
            if (!Integer.valueOf(1).equals(jdbc.queryForObject(
                    "SELECT GET_LOCK(?,0)", Integer.class, lock))) {
                throw new IllegalStateException("该租户已有迁移任务");
            }
            try {
                return migrateLocked(
                        jdbc, tenantId, connectionRef, target, action, administrator, identity(connection));
            } finally {
                try {
                    jdbc.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lock);
                } catch (RuntimeException ignored) {
                    // 连接关闭也会释放，不能掩盖原始失败。
                }
            }
        } catch (SQLException error) {
            throw new IllegalStateException("平台连接不可用，未完成迁移", error);
        }
    }

    private String migrateLocked(JdbcTemplate jdbc, String id, String ref, DataSource target,
                                 Action action, TenantAdministrator administrator,
                                 String platformIdentity) {
        var registrations = jdbc.queryForList("""
                SELECT t.status,d.connection_ref,d.schema_version FROM plt_tenant t
                JOIN plt_tenant_database d ON t.id=d.tenant_id WHERE t.id=?
                """, id);
        if (registrations.size() != 1
                || !ref.equals(registrations.getFirst().get("connection_ref"))) {
            throw new IllegalStateException("租户登记与服务端连接引用不匹配");
        }

        String previousStatus = (String) registrations.getFirst().get("status");
        if (action == Action.NEW
                && !(previousStatus.equals("PROVISIONING") || previousStatus.equals("FAILED"))) {
            throw new IllegalStateException("已开通或停用的租户请使用 UPGRADE，不重新初始化管理员");
        }

        try (var targetConnection = target.getConnection()) {
            if (platformIdentity.equals(identity(targetConnection))) {
                throw new IllegalStateException("平台库与租户库不能指向同一数据库");
            }
        } catch (SQLException error) {
            throw new IllegalStateException("目标连接不可用，未开始迁移", error);
        }

        String operation = UUID.randomUUID().toString();
        var transaction = new TransactionTemplate(new JdbcTransactionManager(jdbc.getDataSource()));
        transaction.executeWithoutResult(tx -> {
            // 已取得同一租户的会话锁，遗留 RUNNING 任务来自中断进程，可记录为失败后重试。
            jdbc.update("UPDATE plt_tenant_operation SET status='FAILED',error_code='INTERRUPTED',"
                    + "finished_at=NOW() WHERE tenant_id=? AND status='RUNNING'", id);
            jdbc.update("INSERT INTO plt_tenant_operation"
                            + "(id,tenant_id,action,phase,status,version_before)"
                            + " VALUES(?,?,?,'PRECHECK','RUNNING',?)",
                    operation, id, action.name(), registrations.getFirst().get("schema_version"));
            jdbc.update("UPDATE plt_tenant_database SET schema_status='UPGRADING' WHERE tenant_id=?", id);
        });

        try {
            var flyway = TenantFlyway.tenant(target);
            boolean managed = flyway.info().current() != null;
            if (managed) {
                if (action == Action.NEW && !"SQL".equals(new JdbcTemplate(target).queryForObject(
                        "SELECT type FROM flyway_tenant_schema_history WHERE version='1'",
                        String.class))) {
                    throw new IllegalStateException("NEW 不接管已做 BASELINE 的旧库");
                }
                String version = flyway.info().current().getVersion().getVersion();
                if (!Set.of("1", "2", TenantFlyway.TENANT_VERSION).contains(version)) {
                    throw new IllegalStateException("目标库版本不受当前迁移器支持");
                }
                SchemaContract.verify(target, "tenant-v" + version);
                checkOwnership(target, id);
            }

            if (!managed) {
                switch (action) {
                    case NEW -> requireEmpty(target);
                    case ADOPT -> {
                        SchemaContract.verify(target, "tenant-v1");
                        rejectFailedLegacyHistory(target);
                        flyway.baseline();
                    }
                    case UPGRADE -> throw new IllegalStateException("尚未接管的库必须显式使用 ADOPT");
                }
            }

            phase(jdbc, operation, "MIGRATE");
            // V2 创建归属表；先写入归属，V3 才能把既有数值记忆 ID 安全映射至本企业。
            var current = flyway.info().current();
            if (current == null
                    || MigrationVersion.fromVersion("2").compareTo(current.getVersion()) > 0) {
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .target("2")
                        .load()
                        .migrate();
            }
            bindOwnership(target, id);
            flyway.migrate();

            if (action == Action.NEW) {
                phase(jdbc, operation, "ADMINISTRATOR");
                administrator.initialize(target);
            }

            phase(jdbc, operation, "VERIFY");
            verify(target, id);
            if (action != Action.UPGRADE) {
                TenantAdministrator.verifyExisting(target);
            }

            transaction.executeWithoutResult(tx -> {
                jdbc.update("UPDATE plt_tenant_database SET schema_status='READY',schema_version=?"
                        + " WHERE tenant_id=?", TenantFlyway.TENANT_VERSION, id);
                jdbc.update("UPDATE plt_tenant SET status=? WHERE id=?",
                        previousStatus.equals("SUSPENDED") ? "SUSPENDED" : "ACTIVE", id);
                jdbc.update("UPDATE plt_tenant_operation SET status='SUCCEEDED',phase='COMPLETE',"
                        + "version_after=?,finished_at=NOW() WHERE id=?",
                        TenantFlyway.TENANT_VERSION, operation);
            });
            return operation;
        } catch (RuntimeException error) {
            try {
                transaction.executeWithoutResult(tx -> {
                    jdbc.update("UPDATE plt_tenant_database SET schema_status='FAILED' WHERE tenant_id=?", id);
                    if (previousStatus.equals("PROVISIONING")) {
                        jdbc.update("UPDATE plt_tenant SET status='FAILED' WHERE id=?", id);
                    }
                    jdbc.update("UPDATE plt_tenant_operation SET status='FAILED',"
                            + "error_code='MIGRATION_FAILED',finished_at=NOW() WHERE id=?", operation);
                });
            } catch (RuntimeException loggingFailure) {
                error.addSuppressed(loggingFailure);
            }
            throw new IllegalStateException(
                    "租户迁移失败；检查平台操作记录与目标历史，不会自动 repair 或回滚 DDL", error);
        }
    }

    public static void verify(DataSource target, String tenantId) {
        requireCurrent(TenantFlyway.tenant(target), TenantFlyway.TENANT_VERSION);
        SchemaContract.verify(target, "tenant-v" + TenantFlyway.TENANT_VERSION);
        String owner = new JdbcTemplate(target).queryForObject(
                "SELECT tenant_id FROM sys_tenant_identity WHERE singleton=1", String.class);
        if (!tenantId.equals(owner)) {
            throw new IllegalStateException("企业数据库归属不匹配");
        }
    }

    private static void requireCurrent(Flyway flyway, String version) {
        flyway.validate();
        var info = flyway.info();
        if (info.current() == null
                || !version.equals(info.current().getVersion().getVersion())
                || info.pending().length != 0) {
            throw new IllegalStateException("schema 版本不匹配，先完成对应轨道迁移");
        }
    }

    private static void phase(JdbcTemplate jdbc, String operation, String phase) {
        jdbc.update("UPDATE plt_tenant_operation SET phase=? WHERE id=?", phase, operation);
    }

    private static void checkOwnership(DataSource target, String id) {
        var jdbc = new JdbcTemplate(target);
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables"
                        + " WHERE table_schema=DATABASE() AND table_name='sys_tenant_identity'",
                Integer.class);
        if (tables != null && tables > 0) {
            var owners = jdbc.queryForList("SELECT tenant_id FROM sys_tenant_identity", String.class);
            if (!owners.isEmpty() && (owners.size() != 1 || !id.equals(owners.getFirst()))) {
                throw new IllegalStateException("目标库已属于另一企业，拒绝迁移");
            }
        }
    }

    private static void bindOwnership(DataSource target, String id) {
        var jdbc = new JdbcTemplate(target);
        new TransactionTemplate(new JdbcTransactionManager(target)).executeWithoutResult(tx -> {
            jdbc.update("INSERT IGNORE INTO sys_tenant_identity(singleton,tenant_id) VALUES(1,?)", id);
            String owner = jdbc.queryForObject(
                    "SELECT tenant_id FROM sys_tenant_identity WHERE singleton=1 FOR UPDATE",
                    String.class);
            if (!id.equals(owner)) {
                throw new IllegalStateException("目标库已属于另一企业");
            }
        });
    }

    private static void requireEmpty(DataSource source) {
        try (var connection = source.getConnection();
             var query = connection.createStatement();
             var rows = query.executeQuery("SHOW FULL TABLES")) {
            if (rows.next()) {
                throw new IllegalStateException("NEW 仅接受空库，已有库必须显式接管");
            }
            if (!SchemaContract.snapshot(connection).isEmpty()) {
                throw new IllegalStateException("目标不是空库");
            }
        } catch (SQLException error) {
            throw new IllegalStateException("空库检查失败", error);
        }
    }

    private static void rejectFailedLegacyHistory(DataSource source) {
        var jdbc = new JdbcTemplate(source);
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables"
                        + " WHERE table_schema=DATABASE() AND table_name='flyway_schema_history'",
                Integer.class);
        if (tables != null
                && tables > 0
                && !Integer.valueOf(0).equals(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success=0", Integer.class))) {
            throw new IllegalStateException("原迁移历史存在失败记录，拒绝接管");
        }
    }

    private static String identity(Connection connection) throws SQLException {
        if (connection.getCatalog() == null || connection.getCatalog().isBlank()) {
            throw new IllegalStateException("连接必须明确目标库名");
        }
        try (var query = connection.createStatement();
             var rows = query.executeQuery("SELECT @@server_uuid")) {
            rows.next();
            return rows.getString(1) + ":" + connection.getCatalog().toLowerCase(Locale.ROOT);
        }
    }
}
