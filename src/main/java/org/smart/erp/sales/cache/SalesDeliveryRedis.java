package org.smart.erp.sales.cache;

import org.smart.erp.common.utils.BaseRedis;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SalesDeliveryRedis extends BaseRedis {

    private static final String DELIVERY_KEY_PREFIX = "erp:sales:delivery:processing:";

    public SalesDeliveryRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, Object value) {
        String key = getRedisKey(DELIVERY_KEY_PREFIX, id);
        return setIfAbsent(key, value, Duration.ofSeconds(30));
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, String token) {
        String key = getRedisKey(DELIVERY_KEY_PREFIX, id);
        return setIfAbsent(key, token, Duration.ofSeconds(30));
    }

    public void evictDeliveryCache(Long id) {
        String key = getRedisKey(DELIVERY_KEY_PREFIX, id);
        deleteCache(key);
    }
}
