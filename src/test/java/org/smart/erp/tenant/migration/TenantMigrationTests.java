package org.smart.erp.tenant.migration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.datasource.TenantDataSourceRegistry;
import org.smart.erp.tenant.registry.JdbcTenantCatalog;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** 真实 Flyway/MySQL 生命周期回归；不回退任何已有业务库。 */
@EnabledIfEnvironmentVariable(named="TENANT_TEST_JDBC_BASE", matches="jdbc:mysql://.+/")
class TenantMigrationTests {
    private final String prefix = "tenant_fw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "_";
    private final List<String> databases = new ArrayList<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private DriverManagerDataSource platform;
    private JdbcTemplate registry;
    private TenantSchemaMigrator migrator;
    @TempDir Path temporary;

    private String url(String schema) { return System.getenv("TENANT_TEST_JDBC_BASE") + schema + "?useSSL=false&allowPublicKeyRetrieval=true"; }
    private DriverManagerDataSource source(String schema) { return new DriverManagerDataSource(url(schema), "root", ""); }
    private DriverManagerDataSource database(String name) {
        String schema = prefix + name;
        new JdbcTemplate(source("")).execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        databases.add(schema);
        return source(schema);
    }
    private DriverManagerDataSource tenant(String id) {
        var target = database(id);
        registry.update("INSERT INTO plt_tenant(id,code,name) VALUES(?,?,?)", id, id, "企业" + id);
        registry.update("INSERT INTO plt_tenant_database(tenant_id,connection_ref) VALUES(?,?)", id, id);
        return target;
    }
    private TenantAdministrator administrator() { return new TenantAdministrator("owner", encoder.encode("deployment-selected-password")); }
    private void create(String id, DataSource target) { migrator.execute(id, id, target, TenantSchemaMigrator.Action.NEW, administrator()); }
    private void legacy(DataSource target) {
        new ResourceDatabasePopulator(new ClassPathResource("db/tenant-migration/V1__tenant_baseline.sql")).execute(target);
        administrator().initialize(target);
    }
    private int count(DataSource target, String table) { return new JdbcTemplate(target).queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private String schemaStatus(String id) { return registry.queryForObject("SELECT schema_status FROM plt_tenant_database WHERE tenant_id=?", String.class, id); }
    private String status(String id) { return registry.queryForObject("SELECT status FROM plt_tenant WHERE id=?", String.class, id); }

    @BeforeEach void setup() {
        platform = database("platform");
        PlatformSchemaMigrator.migrate(platform, false);
        registry = new JdbcTemplate(platform);
        migrator = new TenantSchemaMigrator(platform);
    }
    @AfterEach void cleanup() {
        var admin = new JdbcTemplate(source(""));
        for (String schema : databases) admin.execute("DROP DATABASE IF EXISTS `" + schema + "`");
    }

    @Test void newSchemasHaveIndependentHistoryPublicSeedAndExplicitAdministrator() {
        var a = tenant("a"); var b = tenant("b");
        create("a", a); create("b", b);
        for (var target : List.of(a, b)) {
            assertThat(count(target, "sys_permission")).isEqualTo(117);
            assertThat(count(target, "sys_menu")).isEqualTo(37);
            assertThat(count(target, "sys_role_permission")).isEqualTo(117);
            assertThat(count(target, "sys_role_menu")).isEqualTo(37);
            assertThat(count(target, TenantFlyway.TENANT_HISTORY)).isEqualTo(2);
            var jdbc = new JdbcTemplate(target);
            assertThat(jdbc.queryForObject("SELECT id FROM sys_user WHERE username='owner'", Long.class)).isEqualTo(80001L);
            assertThat(encoder.matches("deployment-selected-password", jdbc.queryForObject("SELECT password FROM sys_user WHERE username='owner'", String.class))).isTrue();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username='admin'", Integer.class)).isZero();
        }
        TenantSchemaMigrator.verify(a, "a"); TenantSchemaMigrator.verify(b, "b");
        assertThat(status("a")).isEqualTo("ACTIVE"); assertThat(schemaStatus("a")).isEqualTo("READY");
        assertThat(count(platform, TenantFlyway.PLATFORM_HISTORY)).isEqualTo(2);
        assertThat(registry.queryForList("SELECT status FROM plt_tenant_operation", String.class)).containsExactly("SUCCEEDED", "SUCCEEDED");
    }

    @Test void replayDoesNotResetPasswordsStatusesOrDuplicateSeed() {
        var target = tenant("a"); create("a", target);
        String changed = encoder.encode("changed-by-enterprise");
        new JdbcTemplate(target).update("UPDATE sys_user SET password=?,status=0 WHERE username='owner'", changed);
        migrator.execute("a", "a", target, TenantSchemaMigrator.Action.UPGRADE, null);
        var row = new JdbcTemplate(target).queryForMap("SELECT password,status FROM sys_user WHERE username='owner'");
        assertThat(row.get("password")).isEqualTo(changed); assertThat(((Number)row.get("status")).intValue()).isZero();
        assertThat(count(target, TenantFlyway.TENANT_HISTORY)).isEqualTo(2);
        assertThat(count(target, "sys_permission")).isEqualTo(117);
        assertThat(count(target, "sys_role_permission")).isEqualTo(117);
    }

    @Test void adoptionPreservesLegacyHistoryPasswordAndBusinessRows() throws Exception {
        var target = tenant("a"); legacy(target);
        String ddl = Files.readString(Path.of("sql/smart-erp/flyway_schema_history.sql")).replace("`smart-erp`.", "");
        new ResourceDatabasePopulator(new ByteArrayResource(ddl.getBytes(java.nio.charset.StandardCharsets.UTF_8))).execute(target);
        var jdbc = new JdbcTemplate(target);
        jdbc.update("INSERT INTO flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) VALUES(1,'20260921','old migration','SQL','old.sql',123,'operator',5,1)");
        jdbc.update("INSERT INTO md_factory(id,name,code,status,create_time,deleted) VALUES(42,'已有工厂','EXISTING',1,NOW(),0)");
        String before = jdbc.queryForObject("SELECT password FROM sys_user WHERE username='owner'", String.class);
        var history = jdbc.queryForList("SELECT * FROM flyway_schema_history");
        migrator.execute("a", "a", target, TenantSchemaMigrator.Action.ADOPT, null);
        assertThat(jdbc.queryForList("SELECT * FROM flyway_schema_history")).isEqualTo(history);
        assertThat(jdbc.queryForObject("SELECT type FROM flyway_tenant_schema_history WHERE version='1'", String.class)).isEqualTo("BASELINE");
        assertThat(jdbc.queryForObject("SELECT password FROM sys_user WHERE username='owner'", String.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT code FROM md_factory WHERE id=42", String.class)).isEqualTo("EXISTING");
        assertThat(count(target, "sys_role_permission")).isEqualTo(117);
        assertThat(schemaStatus("a")).isEqualTo("READY");
    }

    @Test void schemaDifferencePreventsBaselineAndActivationAndCanBeRetried() {
        var target = tenant("a"); legacy(target);
        var jdbc = new JdbcTemplate(target);
        jdbc.execute("ALTER TABLE sys_user ADD COLUMN unexpected VARCHAR(10)");
        assertThatThrownBy(() -> migrator.execute("a", "a", target, TenantSchemaMigrator.Action.ADOPT, null)).isInstanceOf(IllegalStateException.class);
        assertThat(status("a")).isEqualTo("FAILED"); assertThat(schemaStatus("a")).isEqualTo("FAILED");
        assertThat(TenantFlyway.tenant(target).info().current()).isNull();
        jdbc.execute("ALTER TABLE sys_user DROP COLUMN unexpected");
        migrator.execute("a", "a", target, TenantSchemaMigrator.Action.ADOPT, null);
        assertThat(status("a")).isEqualTo("ACTIVE");
        assertThat(registry.queryForList("SELECT status FROM plt_tenant_operation ORDER BY started_at,id", String.class)).containsExactlyInAnyOrder("FAILED", "SUCCEEDED");
    }

    @Test void checksumFailureBlocksOnlyOneTenantAndNeedsExplicitCorrection() {
        var a = tenant("a"); var b = tenant("b"); create("a", a); create("b", b);
        var jdbc = new JdbcTemplate(a);
        int checksum = jdbc.queryForObject("SELECT checksum FROM flyway_tenant_schema_history WHERE version='2'", Integer.class);
        jdbc.update("UPDATE flyway_tenant_schema_history SET checksum=? WHERE version='2'", checksum + 1);
        assertThatThrownBy(() -> migrator.execute("a", "a", a, TenantSchemaMigrator.Action.UPGRADE, null)).isInstanceOf(IllegalStateException.class);
        assertThat(schemaStatus("a")).isEqualTo("FAILED");
        var catalog = new JdbcTenantCatalog(platform);
        assertThatThrownBy(() -> catalog.byId("a")).isInstanceOf(TenantException.class);
        assertThat(catalog.byId("b").id()).isEqualTo("b");
        // 仅测试中还原人为损坏的 checksum；生产工具不自动 repair。
        jdbc.update("UPDATE flyway_tenant_schema_history SET checksum=? WHERE version='2'", checksum);
        migrator.execute("a", "a", a, TenantSchemaMigrator.Action.UPGRADE, null);
        assertThat(schemaStatus("a")).isEqualTo("READY");
    }

    @Test void upgradeNeverReactivatesSuspendedTenant() {
        var target = tenant("a"); create("a", target);
        registry.update("UPDATE plt_tenant SET status='SUSPENDED' WHERE id='a'");
        migrator.execute("a", "a", target, TenantSchemaMigrator.Action.UPGRADE, null);
        assertThat(status("a")).isEqualTo("SUSPENDED"); assertThat(schemaStatus("a")).isEqualTo("READY");
    }

    @Test void anotherOperatorHoldingLockPreventsConcurrentMigration() throws Exception {
        var target = tenant("a");
        String lock = "erp:tm:" + UUID.nameUUIDFromBytes("erp:tenant-migrate:a".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (var connection = platform.getConnection(); var query = connection.prepareStatement("SELECT GET_LOCK(?,0)")) {
            query.setString(1, lock); query.executeQuery().close();
            assertThatThrownBy(() -> create("a", target)).isInstanceOf(IllegalStateException.class).hasMessageContaining("已有迁移任务");
        }
        assertThat(count(platform, "plt_tenant_operation")).isZero(); assertThat(status("a")).isEqualTo("PROVISIONING");
    }

    @Test void incorrectReferenceOrPlatformTargetCannotBeMigrated() {
        var target = tenant("a");
        assertThatThrownBy(() -> migrator.execute("a", "wrong", target, TenantSchemaMigrator.Action.NEW, administrator())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> migrator.execute("a", "a", platform, TenantSchemaMigrator.Action.NEW, administrator())).isInstanceOf(IllegalStateException.class);
        assertThat(count(platform, "plt_tenant_operation")).isZero();
    }

    @Test void physicalSchemaCannotBeReassignedToAnotherTenant() {
        var a = tenant("a"); create("a", a); tenant("b");
        assertThatThrownBy(() -> migrator.execute("b", "b", a, TenantSchemaMigrator.Action.NEW, administrator())).isInstanceOf(IllegalStateException.class);
        assertThat(new JdbcTemplate(a).queryForObject("SELECT tenant_id FROM sys_tenant_identity", String.class)).isEqualTo("a");
        assertThat(status("b")).isEqualTo("FAILED"); TenantSchemaMigrator.verify(a, "a");
    }

    @Test void routingRefusesSwappedRuntimeDatabaseConfiguration() {
        var a = tenant("a"); var b = tenant("b"); create("a", a); create("b", b);
        var properties = new TenantProperties();
        var config = new TenantProperties.Connection(); config.setUrl(b.getUrl()); config.setUsername("root"); config.setPassword("");
        properties.getConnections().put("a", config);
        try (var pools = new TenantDataSourceRegistry(properties)) {
            assertThatThrownBy(() -> pools.get(new JdbcTenantCatalog(platform).byId("a"))).isInstanceOf(TenantException.class);
        }
    }

    @Test void partialDatabaseIsNeverTreatedAsFresh() {
        var target = tenant("a"); new JdbcTemplate(target).execute("CREATE TABLE partial_state(id INT PRIMARY KEY)");
        assertThatThrownBy(() -> create("a", target)).isInstanceOf(IllegalStateException.class);
        assertThat(status("a")).isEqualTo("FAILED"); assertThat(TenantFlyway.tenant(target).info().current()).isNull();
        assertThat(count(target, "partial_state")).isZero();
    }

    @Test void t1RegistryIsAdoptedOnlyAfterExplicitStructureCheck() {
        var old = database("old_platform");
        new ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource("sql/platform/01_tenant_registry.sql")).execute(old);
        new JdbcTemplate(old).update("INSERT INTO plt_tenant(id,code,name) VALUES('existing','existing','既有企业')");
        assertThatThrownBy(() -> PlatformSchemaMigrator.migrate(old, false)).isInstanceOf(RuntimeException.class);
        PlatformSchemaMigrator.migrate(old, true);
        PlatformSchemaMigrator.migrate(old, true);
        assertThat(new JdbcTemplate(old).queryForObject("SELECT type FROM flyway_platform_schema_history WHERE version='1'", String.class)).isEqualTo("BASELINE");
        assertThat(new JdbcTemplate(old).queryForObject("SELECT name FROM plt_tenant WHERE id='existing'", String.class)).isEqualTo("既有企业");
    }

    @Test void unknownPlatformStructureCannotBeBaselined() {
        var old = database("unknown_platform");
        new JdbcTemplate(old).execute("CREATE TABLE unknown_table(id INT PRIMARY KEY)");
        assertThatThrownBy(() -> PlatformSchemaMigrator.migrate(old, true)).isInstanceOf(IllegalStateException.class);
        assertThat(TenantFlyway.platform(old).info().current()).isNull();
    }

    @Test void newTenantWithoutExplicitAdministratorIsRejectedBeforeChanges() {
        var target = tenant("a");
        assertThatThrownBy(() -> migrator.execute("a", "a", target, TenantSchemaMigrator.Action.NEW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(count(platform, "plt_tenant_operation")).isZero(); assertThat(status("a")).isEqualTo("PROVISIONING");
    }

    @Test void retryRecordsInterruptedOperationBeforeCompleting() {
        var target = tenant("a");
        org.flywaydb.core.Flyway.configure().dataSource(target).locations("classpath:db/tenant-migration")
                .table(TenantFlyway.TENANT_HISTORY).target("1").load().migrate();
        registry.update("INSERT INTO plt_tenant_operation(id,tenant_id,action,phase,status) VALUES('abandoned','a','NEW','MIGRATE','RUNNING')");
        create("a", target);
        assertThat(registry.queryForObject("SELECT error_code FROM plt_tenant_operation WHERE id='abandoned'", String.class)).isEqualTo("INTERRUPTED");
        assertThat(registry.queryForObject("SELECT status FROM plt_tenant_operation WHERE id='abandoned'", String.class)).isEqualTo("FAILED");
        assertThat(status("a")).isEqualTo("ACTIVE");
        assertThat(count(target, TenantFlyway.TENANT_HISTORY)).isEqualTo(2);
        assertThat(count(target, "sys_user")).isEqualTo(1);
    }

    @Test void commandUsesExternalRuntimePropertiesAndExplicitTenant() throws Exception {
        var target = tenant("a");
        registry.update("UPDATE plt_tenant_database SET connection_ref='enterprise_a' WHERE tenant_id='a'");
        var config = new Properties();
        config.setProperty("tenancy.platform.url", platform.getUrl());
        config.setProperty("tenancy.platform.username", "root"); config.setProperty("tenancy.platform.password", "");
        config.setProperty("tenancy.connections.enterprise_a.url", target.getUrl());
        config.setProperty("tenancy.connections.enterprise_a.username", "root"); config.setProperty("tenancy.connections.enterprise_a.password", "");
        config.setProperty("TENANT_ADMIN_USERNAME", "owner"); config.setProperty("TENANT_ADMIN_BCRYPT", encoder.encode("operator-chosen-password"));
        Path file = temporary.resolve("external.properties");
        try (var writer = Files.newBufferedWriter(file)) { config.store(writer, "test only"); }
        TenantMigrationCommand.run(new String[]{"new", file.toString(), "a"});
        TenantMigrationCommand.run(new String[]{"verify", file.toString(), "a"});
        TenantMigrationCommand.run(new String[]{"upgrade", file.toString(), "a"});
        assertThat(status("a")).isEqualTo("ACTIVE");
    }
}
