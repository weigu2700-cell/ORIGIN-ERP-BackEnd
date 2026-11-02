package org.smart.erp.tenant.config;

import org.smart.erp.tenant.datasource.*;
import org.smart.erp.tenant.registry.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name="tenancy.mode", havingValue="multi")
public class MultiTenantDataSourceConfiguration {
    @Bean(name="platformDataSource", destroyMethod="close")
    public DataSource platformDataSource(TenantProperties properties, Environment env) {
        if (env.getProperty("spring.flyway.enabled", Boolean.class, true)
                || !"never".equals(env.getProperty("spring.ai.chat.memory.repository.jdbc.initialize-schema"))
                || !"never".equals(env.getProperty("spring.sql.init.mode")))
            throw new IllegalStateException("多租户验证阶段必须关闭业务数据源的 Flyway、SQL init 和 AI 自动建表");
        return TenantDataSourceRegistry.create(properties.getPlatform(), "tenant-platform", 2);
    }
    @Bean public TenantCatalog tenantCatalog(@Qualifier("platformDataSource") DataSource platform) { return new JdbcTenantCatalog(platform); }
    @Bean(destroyMethod="close") public TenantDataSourceRegistry tenantDataSourceRegistry(TenantProperties properties) { return new TenantDataSourceRegistry(properties); }
    @Bean(name="dataSource") @Primary
    public DataSource tenantDataSource(TenantCatalog catalog, TenantDataSourceRegistry registry) { return new TenantRoutingDataSource(catalog, registry); }
    @Bean
    public org.springframework.ai.chat.memory.ChatMemory tenantChatMemory(
            @Qualifier("dataSource") DataSource dataSource, org.smart.erp.tenant.context.TenantExecutor tenants) {
        return new org.smart.erp.tenant.context.TenantJdbcChatMemory(dataSource, tenants);
    }

}
