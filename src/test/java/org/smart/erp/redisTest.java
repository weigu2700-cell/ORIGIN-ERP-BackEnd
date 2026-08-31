package org.smart.erp;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class redisTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void generateOrderNo() {
        int maxRetry =3;
        for (int i = 0; i < maxRetry; i++) {
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String seqKey = "order:seq:" + datePrefix;
            Long seq = stringRedisTemplate.opsForValue().increment(seqKey, 1);
            stringRedisTemplate.expire(seqKey, 3, TimeUnit.DAYS);
            String orderNo = "SO" + datePrefix + String.format("%04d", seq);
            System.out.println(orderNo);
        }
    }
}
