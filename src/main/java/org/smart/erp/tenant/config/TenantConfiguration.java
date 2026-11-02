package org.smart.erp.tenant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 启用多租户配置属性绑定。 */
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfiguration {
}
