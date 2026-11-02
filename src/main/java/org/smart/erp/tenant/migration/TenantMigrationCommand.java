package org.smart.erp.tenant.migration;

import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.datasource.TenantDataSourceRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** 独立 main，不启动 Web、Redis、AI，也不自动迁移全体企业。 */
public final class TenantMigrationCommand {
    private TenantMigrationCommand() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception error) {
            // JDBC/Flyway 异常可能包含连接详情，面向操作员的输出不打印凭据或密码哈希。
            System.err.println("操作失败：请检查配置、平台 plt_tenant_operation、schema 差异及 Flyway 历史；不会自动 repair/clean。");
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if ((cause.getClass() == IllegalStateException.class || cause.getClass() == IllegalArgumentException.class)
                        && cause.getStackTrace().length > 0 && cause.getStackTrace()[0].getClassName().startsWith("org.smart.erp.tenant."))
                    System.err.println(cause.getMessage());
            }
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3)
            throw new IllegalArgumentException("用法：<platform-init|platform-adopt|new|adopt|upgrade|verify> <外部.properties> [tenantId]");
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[1]), StandardCharsets.UTF_8)) { properties.load(reader); }
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new PropertiesPropertySource("tenant-migration", properties));
        var config = Binder.get(environment).bind("tenancy", Bindable.of(TenantProperties.class)).orElseThrow(() -> new IllegalArgumentException("缺少 tenancy 配置"));
        // 和运行时共用配置/地址去重校验；此注册器不创建业务池。
        try (var validation = new TenantDataSourceRegistry(config);
             var platform = TenantDataSourceRegistry.create(config.getPlatform(), "migration-platform", 2)) {
            if (args[0].equals("platform-init") || args[0].equals("platform-adopt")) {
                if (args.length != 2) throw new IllegalArgumentException("平台迁移不接受 tenantId");
                PlatformSchemaMigrator.migrate(platform, args[0].equals("platform-adopt"));
                System.out.println("平台 schema 已就绪，版本 2");
                return;
            }
            if (args.length != 3) throw new IllegalArgumentException("必须显式选择单个租户");
            String id = args[2];
            var references = new JdbcTemplate(platform).queryForList("SELECT connection_ref FROM plt_tenant_database WHERE tenant_id=?", String.class, id);
            if (references.size() != 1 || !config.getConnections().containsKey(references.getFirst()))
                throw new IllegalArgumentException("租户或服务端连接配置不存在");
            String ref = references.getFirst();
            try (var target = TenantDataSourceRegistry.create(config.getConnections().get(ref), "migration-tenant", 2)) {
                if (args[0].equals("verify")) {
                    TenantSchemaMigrator.verify(target, id);
                    System.out.println("租户 schema/历史校验通过，版本 " + TenantFlyway.TENANT_VERSION);
                    return;
                }
                var action = TenantSchemaMigrator.Action.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
                TenantAdministrator admin = action == TenantSchemaMigrator.Action.NEW
                        ? new TenantAdministrator(environment.getRequiredProperty("TENANT_ADMIN_USERNAME"), environment.getRequiredProperty("TENANT_ADMIN_BCRYPT")) : null;
                String operation = new TenantSchemaMigrator(platform).execute(id, ref, target, action, admin);
                System.out.println("租户迁移完成，operation=" + operation + "，版本 " + TenantFlyway.TENANT_VERSION);
            }
        }
    }
}
