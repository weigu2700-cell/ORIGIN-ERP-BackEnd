package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis List（opsForList）类型操作工具类。
 * 提供两套 API：
 *  1) 按 cacheKey + id 的实体维度缓存（自动拼接 key）；
 *  2) 基于完整 key 的全局/自定义缓存。
 * 注意：TTL 作用在整个 list key 上，而非单个元素。
 */
@Component
public class OperationList extends OperationRedis {

    public OperationList(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    private ListOperations<String, Object> ops() {
        return redisTemplate.opsForList();
    }

    // ===================== 按 cacheKey + id 的实体维度缓存 =====================

    public <T> Long leftPush(String keyPrefix, Long id, T value, Duration ttl) {
        if (value == null || id == null) return 0L;
        String key = getRedisKey(keyPrefix, id);
        Long size = ops().leftPush(key, value);
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    public <T> Long rightPush(String keyPrefix, Long id, T value, Duration ttl) {
        if (value == null || id == null) return 0L;
        String key = getRedisKey(keyPrefix, id);
        Long size = ops().rightPush(key, value);
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    public <T> List<T> range(String keyPrefix, Long id, long start, long end) {
        return castToType(ops().range(getRedisKey(keyPrefix, id), start, end));
    }

    public <T> T index(String keyPrefix, Long id, long index) {
        return castToType(ops().index(getRedisKey(keyPrefix, id), index));
    }

    public long size(String keyPrefix, Long id) {
        Long size = ops().size(getRedisKey(keyPrefix, id));
        return size == null ? 0L : size;
    }

    public <T> void set(String keyPrefix, Long id, long index, T value) {
        if (value == null || id == null) return;
        ops().set(getRedisKey(keyPrefix, id), index, value);
    }

    public <T> Long remove(String keyPrefix, Long id, long count, T value) {
        if (value == null || id == null) return 0L;
        Long removed = ops().remove(getRedisKey(keyPrefix, id), count, value);
        return removed == null ? 0L : removed;
    }

    public <T> T leftPop(String keyPrefix, Long id) {
        return castToType(ops().leftPop(getRedisKey(keyPrefix, id)));
    }

    public <T> T rightPop(String keyPrefix, Long id) {
        return castToType(ops().rightPop(getRedisKey(keyPrefix, id)));
    }

    public void delete(String keyPrefix, Long id) {
        redisTemplate.delete(getRedisKey(keyPrefix, id));
    }

    // ===================== 基于完整 key 的全局/自定义缓存 =====================

    public <T> Long leftPush(String key, T value, Duration ttl) {
        if (value == null) return 0L;
        Long size = ops().leftPush(key, value);
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    public <T> Long rightPush(String key, T value, Duration ttl) {
        if (value == null) return 0L;
        Long size = ops().rightPush(key, value);
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    public <T> List<T> range(String key, long start, long end) {
        return castToType(ops().range(key, start, end));
    }

    public <T> T index(String key, long index) {
        return castToType(ops().index(key, index));
    }

    public long size(String key) {
        Long size = ops().size(key);
        return size == null ? 0L : size;
    }

    public <T> void set(String key, long index, T value) {
        if (value == null) return;
        ops().set(key, index, value);
    }

    public <T> Long remove(String key, long count, T value) {
        if (value == null) return 0L;
        Long removed = ops().remove(key, count, value);
        return removed == null ? 0L : removed;
    }

    public <T> T leftPop(String key) {
        return castToType(ops().leftPop(key));
    }

    public <T> T rightPop(String key) {
        return castToType(ops().rightPop(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> Long rightPushAll(String keyPrefix, Long id, List<T> values, Duration ttl) {
        if (values == null || values.isEmpty() || id == null) return 0L;
        String key = getRedisKey(keyPrefix, id);
        Long size = ops().rightPushAll(key, values.toArray());
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    public <T> Long rightPushAll(String key, List<T> values, Duration ttl) {
        if (values == null || values.isEmpty()) return 0L;
        Long size = ops().rightPushAll(key, values.toArray());
        expireIfNeeded(key, ttl);
        return size == null ? 0L : size;
    }

    private void expireIfNeeded(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }
}
