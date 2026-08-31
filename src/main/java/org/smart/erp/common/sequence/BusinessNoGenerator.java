package org.smart.erp.common.sequence;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class BusinessNoGenerator {

    private final StringRedisTemplate stringRedisTemplate;

    public BusinessNoGenerator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String generateOrderNo() {
        String dataPrefix = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String seqKey = "erp:sequence:sales-order:" + dataPrefix;
        Long seq = stringRedisTemplate.opsForValue().increment(seqKey, 1);
        if (seq == null) {
            throw new IllegalStateException("生成销售订单号失败");
        }
        if (seq == 1L) {
            stringRedisTemplate.expire(seqKey, 3, TimeUnit.DAYS);
        }
        return "SO" + dataPrefix + String.format("%04d", seq);
    }
}
