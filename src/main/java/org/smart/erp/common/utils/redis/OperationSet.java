package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis Set（opsForSet）类型操作工具类。
 * 提供两套 API：
 *  1) 按 cacheKey + id 的实体维度缓存（自动拼接 key）；
 *  2) 基于完整 key 的全局/自定义缓存。
 * 注意：TTL 作用在整个 set key 上，而非单个成员。
 */
@Component
public class OperationSet extends OperationRedis {

    private final RedisTemplate<String, Object> redisTemplate;

    public OperationSet(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ===================== 按 cacheKey + id 的实体维度缓存 =====================

    /**
     * 添加成员；返回是否“新”加入（已存在则返回 false）。
     */
    public <T> boolean add(String keyPrefix, Long id, T value, Duration ttl) {
        if (value == null || id == null) return false;
        String key = getRedisKey(keyPrefix, id);
        Long added = redisTemplate.opsForSet().add(key, value);
        expireIfNeeded(key, ttl);
        return added != null && added > 0;
    }

    public <T> Set<T> members(String keyPrefix, Long id) {
        return castToType(redisTemplate.opsForSet().members(getRedisKey(keyPrefix, id)));
    }

    public <T> boolean isMember(String keyPrefix, Long id, T value) {
        if (value == null || id == null) return false;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getRedisKey(keyPrefix, id), value));
    }

    public long size(String keyPrefix, Long id) {
        Long size = redisTemplate.opsForSet().size(getRedisKey(keyPrefix, id));
        return size == null ? 0L : size;
    }

    public <T> boolean remove(String keyPrefix, Long id, T value) {
        if (value == null || id == null) return false;
        Long removed = redisTemplate.opsForSet().remove(getRedisKey(keyPrefix, id), value);
        return removed != null && removed > 0;
    }

    public <T> T pop(String keyPrefix, Long id) {
        return castToType(redisTemplate.opsForSet().pop(getRedisKey(keyPrefix, id)));
    }

    public void delete(String keyPrefix, Long id) {
        redisTemplate.delete(getRedisKey(keyPrefix, id));
    }

    // ===================== 基于完整 key 的全局/自定义缓存 =====================

    public <T> boolean add(String key, T value, Duration ttl) {
        if (value == null) return false;
        Long added = redisTemplate.opsForSet().add(key, value);
        expireIfNeeded(key, ttl);
        return added != null && added > 0;
    }

    public <T> Set<T> members(String key) {
        return castToType(redisTemplate.opsForSet().members(key));
    }

    public <T> boolean isMember(String key, T value) {
        if (value == null) return false;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public long size(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    public <T> boolean remove(String key, T value) {
        if (value == null) return false;
        Long removed = redisTemplate.opsForSet().remove(key, value);
        return removed != null && removed > 0;
    }

    public <T> T pop(String key) {
        return castToType(redisTemplate.opsForSet().pop(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> void addAll(String keyPrefix, Long id, Set<T> values, Duration ttl) {
        if (values == null || values.isEmpty() || id == null) return;
        String key = getRedisKey(keyPrefix, id);
        redisTemplate.opsForSet().add(key, values.toArray());
        expireIfNeeded(key, ttl);
    }

    public <T> void addAll(String key, Set<T> values, Duration ttl) {
        if (values == null || values.isEmpty()) return;
        redisTemplate.opsForSet().add(key, values.toArray());
        expireIfNeeded(key, ttl);
    }

    private void expireIfNeeded(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }
}
