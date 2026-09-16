package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis ZSet（opsForZSet，有序集合）类型操作工具类。
 * 提供两套 API：
 *  1) 按 cacheKey + id 的实体维度缓存（自动拼接 key）；
 *  2) 基于完整 key 的全局/自定义缓存。
 * 注意：TTL 作用在整个 zset key 上，而非单个成员。
 */
@Component
public class OperationZSet extends OperationRedis {

    public OperationZSet(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    // ===================== 按 cacheKey + id 的实体维度缓存 =====================

    /**
     * 添加带分值的成员；返回是否“新”加入（已存在则更新分值并返回 false）。
     */
    public <T> boolean add(String keyPrefix, Long id, T value, double score, Duration ttl) {
        if (value == null || id == null) return false;
        String key = getRedisKey(keyPrefix, id);
        Boolean added = redisTemplate.opsForZSet().add(key, value, score);
        expireIfNeeded(key, ttl);
        return Boolean.TRUE.equals(added);
    }

    public <T> Set<T> range(String keyPrefix, Long id, long start, long end) {
        return castToType(redisTemplate.opsForZSet().range(getRedisKey(keyPrefix, id), start, end));
    }

    public <T> Set<T> rangeByScore(String keyPrefix, Long id, double min, double max) {
        return castToType(redisTemplate.opsForZSet().rangeByScore(getRedisKey(keyPrefix, id), min, max));
    }

    public <T> Double score(String keyPrefix, Long id, T value) {
        if (value == null || id == null) return null;
        return redisTemplate.opsForZSet().score(getRedisKey(keyPrefix, id), value);
    }

    public <T> Double incrementScore(String keyPrefix, Long id, T value, double delta) {
        if (value == null || id == null) return null;
        return redisTemplate.opsForZSet().incrementScore(getRedisKey(keyPrefix, id), value, delta);
    }

    public long size(String keyPrefix, Long id) {
        Long size = redisTemplate.opsForZSet().size(getRedisKey(keyPrefix, id));
        return size == null ? 0L : size;
    }

    public <T> boolean remove(String keyPrefix, Long id, T value) {
        if (value == null || id == null) return false;
        Long removed = redisTemplate.opsForZSet().remove(getRedisKey(keyPrefix, id), value);
        return removed != null && removed > 0;
    }

    public void delete(String keyPrefix, Long id) {
        redisTemplate.delete(getRedisKey(keyPrefix, id));
    }

    // ===================== 基于完整 key 的全局/自定义缓存 =====================

    public <T> boolean add(String key, T value, double score, Duration ttl) {
        if (value == null) return false;
        Boolean added = redisTemplate.opsForZSet().add(key, value, score);
        expireIfNeeded(key, ttl);
        return Boolean.TRUE.equals(added);
    }

    public <T> Set<T> range(String key, long start, long end) {
        return castToType(redisTemplate.opsForZSet().range(key, start, end));
    }

    public <T> Set<T> rangeByScore(String key, double min, double max) {
        return castToType(redisTemplate.opsForZSet().rangeByScore(key, min, max));
    }

    public <T> Double score(String key, T value) {
        if (value == null) return null;
        return redisTemplate.opsForZSet().score(key, value);
    }

    public <T> Double incrementScore(String key, T value, double delta) {
        if (value == null) return null;
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    public long size(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0L : size;
    }

    public <T> boolean remove(String key, T value) {
        if (value == null) return false;
        Long removed = redisTemplate.opsForZSet().remove(key, value);
        return removed != null && removed > 0;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> void addAll(String keyPrefix, Long id, Set<T> values, Duration ttl) {
        if (values == null || values.isEmpty() || id == null) return;
        String key = getRedisKey(keyPrefix, id);
        redisTemplate.opsForZSet().add(key, values.toArray(), 1.0);
        expireIfNeeded(key, ttl);
    }

    public <T> void addAll(String key, Set<T> values, Duration ttl) {
        if (values == null || values.isEmpty()) return;
        redisTemplate.opsForZSet().add(key, values.toArray(), 1.0);
        expireIfNeeded(key, ttl);
    }

    private void expireIfNeeded(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }
}
