package org.smart.erp.tenant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用 Spring 定时任务支持。 */
@Configuration
@EnableScheduling
public class TenantSchedulingConfiguration {
}
