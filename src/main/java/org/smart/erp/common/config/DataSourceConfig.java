package org.smart.erp.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 数据源显式配置。
 *
 * <p>正常情况下由 Spring Boot 的 {@code DataSourceAutoConfiguration} 完成；当自动配置未生效
 * （例如当前使用的 spring-boot-autoconfigure 构建中未包含该配置类）时，此处兜底创建一个
 * {@link DataSource}，使 MyBatis-Plus 自动配置能够继续创建 sqlSessionFactory / sqlSessionTemplate。
 *
 * <p>这里直接基于 {@link Environment} 读取 {@code spring.datasource.*} 并构造 HikariDataSource，
 * 显式将 {@code url} 映射到 {@code setJdbcUrl}（Hikari 没有 {@code setUrl}，避免
 * “jdbcUrl is required with driverClassName” 的问题）。
 *
 * <p>{@link ConditionalOnMissingBean} 确保：若框架已提供 DataSource，则本 Bean 不生效，避免冲突。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(Environment env) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(env.getProperty("spring.datasource.url"));
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));
        return dataSource;
    }
}
