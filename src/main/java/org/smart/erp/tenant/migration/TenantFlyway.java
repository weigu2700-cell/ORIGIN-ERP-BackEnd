package org.smart.erp.tenant.migration;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/** 运维入口直接绑定目标数据源，绝不经过请求路由或全局默认库。 */
public final class TenantFlyway {

    public static final String TENANT_VERSION = "4";
    public static final String TENANT_HISTORY = "flyway_tenant_schema_history";
    public static final String PLATFORM_HISTORY = "flyway_platform_schema_history";

    private TenantFlyway() {
    }

    public static Flyway tenant(DataSource source) {
        return configured(source, "classpath:db/tenant-migration", TENANT_HISTORY);
    }

    public static Flyway platform(DataSource source) {
        return configured(source, "classpath:db/platform-migration", PLATFORM_HISTORY);
    }

    private static Flyway configured(DataSource source, String location, String history) {
        return Flyway.configure()
                .dataSource(source)
                .locations(location)
                .table(history)
                .baselineOnMigrate(false)
                .baselineVersion("1")
                .cleanDisabled(true)
                .createSchemas(false)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .placeholderReplacement(false)
                .load();
    }
}
