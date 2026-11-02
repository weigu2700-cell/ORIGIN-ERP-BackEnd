package org.smart.erp;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SmartErpServerApplicationTests extends org.smart.erp.support.MySqlIntegrationTest {

	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}

}
