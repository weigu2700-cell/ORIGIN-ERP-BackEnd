package org.smart.erp.sales.cache;

import org.smart.erp.common.utils.redis.OperationString;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class SalesDeliveryRedis {

    private static final String DELIVERY_KEY_PREFIX = "erp:sales:delivery:processing:";

    /**
     * 原子释放分布式锁脚本：仅当锁仍由当前 token 持有者占用时才删除，
     * 避免「锁超时自动过期 -> 被其他请求获取 -> 原请求结束时误删他人锁」的经典问题。
     * KEYS[1] = 锁 key；ARGV[1] = 当前请求持有的 token。
     * 返回 1 表示已删除，0 表示锁不存在或已被其他持有者占用（不删除）。
     */
    private static final String RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "  return redis.call('del', KEYS[1])\n" +
            "end\n" +
            "return 0";

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(RELEASE_LOCK_LUA, Long.class);

    private final OperationString operationString;
    private final RedisTemplate<String, Object> redisTemplate;

    public SalesDeliveryRedis(OperationString operationString, RedisTemplate<String, Object> redisTemplate) {
        this.operationString = operationString;
        this.redisTemplate = redisTemplate;
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, Object value) {
        return operationString.setIfAbsent(DELIVERY_KEY_PREFIX, id, value, Duration.ofSeconds(30));
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, String token) {
        return operationString.setIfAbsent(DELIVERY_KEY_PREFIX, id, token, Duration.ofSeconds(30));
    }

    /**
     * 原子释放分布式锁：仅当锁仍由当前请求持有的 token 占用时才删除，避免误删他人锁。
     * @param id 发货单ID
     * @param token 本请求获取锁时持有的 token（即 {@link #setDeliveryCacheIfAbsent} 写入的值）
     * @return true=锁确实由本请求持有并已删除
     */
    public boolean releaseDeliveryLock(Long id, String token) {
        if (id == null || token == null) {
            return false;
        }
        String key = DELIVERY_KEY_PREFIX + id;
        Long result = redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), token);
        return Long.valueOf(1L).equals(result);
    }
}
