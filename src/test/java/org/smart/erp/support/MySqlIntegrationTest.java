package org.smart.erp.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Integration tests must never fall back to the developer's business database. */
@Testcontainers
public abstract class MySqlIntegrationTest {
    @Container
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("origin_erp_test").withUsername("origin").withPassword("origin");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        // These tests create their own schema. Migration verification needs a separate baseline fixture.
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.ai.chat.memory.repository.jdbc.initialize-schema", () -> "never");
        registry.add("spring.ai.openai.api-key", () -> "local-test-only");
        registry.add("spring.data.redis.repositories.enabled", () -> false);
        registry.add("security.jwt.secret", () -> "c21hcnQtZXJwLXRlc3Qtc2VjcmV0LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXM=");
    }
}
