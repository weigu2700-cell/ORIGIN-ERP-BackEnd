package org.smart.erp.inventory.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.smart.erp.inventory.service.MaterialStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MaterialStockConcurrencyIntegrationTests {

	private static final long MATERIAL_ID = 11L;

	private static final long WAREHOUSE_ID = 21L;

	@Container
	private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("origin_erp_test")
		.withUsername("origin")
		.withPassword("origin");

	@DynamicPropertySource
	static void mysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
		registry.add("spring.data.redis.repositories.enabled", () -> false);
		registry.add("security.jwt.secret", () -> "c21hcnQtZXJwLXRlc3Qtc2VjcmV0LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXM=");
	}

	@MockitoBean
	private RedissonClient redissonClient;

	@Autowired
	private MaterialStockService materialStockService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createSchema() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS inv_transaction");
		jdbcTemplate.execute("DROP TABLE IF EXISTS inv_material_stock");
		jdbcTemplate.execute("""
				CREATE TABLE inv_material_stock (
				    id BIGINT NOT NULL PRIMARY KEY,
				    warehouse_id BIGINT NOT NULL,
				    material_id BIGINT NOT NULL,
				    on_hand DECIMAL(18, 4) NOT NULL,
				    reserved DECIMAL(18, 4) NOT NULL,
				    version INT NOT NULL DEFAULT 0,
				    create_time DATETIME NOT NULL,
				    update_time DATETIME NULL,
				    CONSTRAINT uk_material_warehouse_id UNIQUE (material_id, warehouse_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE inv_transaction (
				    id BIGINT NOT NULL PRIMARY KEY,
				    warehouse_id BIGINT NOT NULL,
				    material_id BIGINT NOT NULL,
				    transaction_type TINYINT NOT NULL,
				    business_type VARCHAR(64) NOT NULL,
				    business_no VARCHAR(100) NOT NULL,
				    quantity DECIMAL(18, 4) NOT NULL,
				    before_on_hand DECIMAL(18, 4) NOT NULL,
				    after_on_hand DECIMAL(18, 4) NOT NULL,
				    before_reserved DECIMAL(18, 4) NOT NULL,
				    after_reserved DECIMAL(18, 4) NOT NULL,
				    remark VARCHAR(255) NULL,
				    create_time DATETIME NOT NULL
				)
				""");
	}

	@Test
	void concurrentFirstInboundCreatesOneStockRowAndTwoLedgerRows() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<?> first = executor
				.submit(() -> inboundAfterBarrier(ready, start, new BigDecimal("2"), "PI-CONCURRENT-1"));
			Future<?> second = executor
				.submit(() -> inboundAfterBarrier(ready, start, new BigDecimal("3"), "PI-CONCURRENT-2"));

			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			first.get(30, TimeUnit.SECONDS);
			second.get(30, TimeUnit.SECONDS);
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM inv_material_stock WHERE material_id = ? AND warehouse_id = ?", Long.class,
				MATERIAL_ID, WAREHOUSE_ID))
			.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT on_hand FROM inv_material_stock WHERE material_id = ? AND warehouse_id = ?", BigDecimal.class,
				MATERIAL_ID, WAREHOUSE_ID))
			.isEqualByComparingTo("5");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT version FROM inv_material_stock WHERE material_id = ? AND warehouse_id = ?", Integer.class,
				MATERIAL_ID, WAREHOUSE_ID))
			.isEqualTo(1);

		List<Map<String, Object>> ledger = jdbcTemplate.queryForList("""
				SELECT quantity, before_on_hand, after_on_hand
				FROM inv_transaction
				WHERE material_id = ? AND warehouse_id = ?
				ORDER BY after_on_hand
				""", MATERIAL_ID, WAREHOUSE_ID);
		assertThat(ledger).hasSize(2);
		assertThat(ledger).extracting(row -> (BigDecimal) row.get("quantity"))
			.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
			.containsExactlyInAnyOrder(new BigDecimal("2"), new BigDecimal("3"));
		BigDecimal firstBefore = (BigDecimal) ledger.getFirst().get("before_on_hand");
		BigDecimal firstAfter = (BigDecimal) ledger.getFirst().get("after_on_hand");
		BigDecimal secondBefore = (BigDecimal) ledger.getLast().get("before_on_hand");
		BigDecimal secondAfter = (BigDecimal) ledger.getLast().get("after_on_hand");
		assertThat(firstBefore).isEqualByComparingTo("0");
		assertThat(firstAfter).isEqualByComparingTo(secondBefore);
		assertThat(secondAfter).isEqualByComparingTo("5");
		assertThat(firstAfter.subtract(firstBefore))
			.isEqualByComparingTo((BigDecimal) ledger.getFirst().get("quantity"));
		assertThat(secondAfter.subtract(secondBefore))
			.isEqualByComparingTo((BigDecimal) ledger.getLast().get("quantity"));
	}

	private void inboundAfterBarrier(CountDownLatch ready, CountDownLatch start, BigDecimal quantity,
			String businessNo) {
		try {
			ready.countDown();
			if (!start.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("并发入库启动超时");
			}
			materialStockService.inboundStock(MATERIAL_ID, WAREHOUSE_ID, quantity, "PURCHASE_IN_STOCK", businessNo,
					"采购并发首次入库测试");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("并发入库测试被中断", e);
		}
	}

}
