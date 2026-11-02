package org.smart.erp.tenant.migration;

import javax.sql.DataSource;

public final class PlatformSchemaMigrator {
    private PlatformSchemaMigrator() {}

    /** T1 的手工登记库必须显式校验后接管，空库不做 baseline。 */
    public static void migrate(DataSource platform, boolean adoptRegistry) {
        var flyway = TenantFlyway.platform(platform);
        if (adoptRegistry && flyway.info().current() == null) {
            SchemaContract.verify(platform, "platform-v1");
            flyway.baseline();
        }
        if (flyway.info().current() != null) {
            String version = flyway.info().current().getVersion().getVersion();
            if (!java.util.Set.of("1", "2").contains(version)) throw new IllegalStateException("平台 schema 版本不受支持");
            SchemaContract.verify(platform, "platform-v" + version);
        }
        flyway.migrate();
        flyway.validate();
        SchemaContract.verify(platform, "platform-v2");
    }
}
