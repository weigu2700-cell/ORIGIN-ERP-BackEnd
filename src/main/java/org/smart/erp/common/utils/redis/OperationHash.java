package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Redis Hash（opsForHash）类型操作工具类。
 * 提供两套 API：
 *  1) 按 cacheKey + id 的实体维度缓存（自动拼接 key）；
 *  2) 基于完整 key 的全局/自定义缓存。
 * 注意：Redis 的 TTL 是 key 级别而非 field 级别，因此过期时间作用在整个 hash key 上。
 */
@Component
public class OperationHash extends OperationRedis {

    private final RedisTemplate<String, Object> redisTemplate;

    public OperationHash(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ===================== 按 cacheKey + id 的实体维度缓存 =====================

    public <K> boolean hasKey(String keyPrefix, Long id, K field) {
        return redisTemplate.opsForHash().hasKey(getRedisKey(keyPrefix, id), field);
    }

    public <K, V> void put(String keyPrefix, Long id, K field, V value, Duration ttl) {
        if (value == null) return;
        String key = getRedisKey(keyPrefix, id);
        redisTemplate.opsForHash().put(key, field, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <K, V> V get(String keyPrefix, Long id, K field) {
        return castToType(redisTemplate.opsForHash().get(getRedisKey(keyPrefix, id), field));
    }

    public <K> void delete(String keyPrefix, Long id, K field) {
        redisTemplate.opsForHash().delete(getRedisKey(keyPrefix, id), field);
    }

    /**
     * NX 写入单个 hash field：field 不存在时才写入，返回是否真正写入成功。
     * 仅在本次确实写入时才设置 TTL，避免重复调用把已存在 key 的 TTL 反复续期。
     */
    public <K, V> boolean putIfAbsent(String keyPrefix, Long id, K field, V value, Duration ttl) {
        if (field == null || value == null) return false;
        String key = getRedisKey(keyPrefix, id);
        Boolean set = redisTemplate.opsForHash().putIfAbsent(key, field, value);
        if (Boolean.TRUE.equals(set) && ttl != null) {
            redisTemplate.expire(key, ttl);
        }
        return Boolean.TRUE.equals(set);
    }

    // ===================== 基于完整 key 的全局/自定义缓存 =====================

    public <K> boolean hasKey(String key, K field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    public <K, V> void put(String key, K field, V value, Duration ttl) {
        if (value == null) return;
        redisTemplate.opsForHash().put(key, field, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <K, V> V get(String key, K field) {
        return castToType(redisTemplate.opsForHash().get(key, field));
    }

    public <K> void delete(String key, K field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    /**
     * NX 写入单个 hash field（基于完整 key）。仅在本次确实写入时才设置 TTL。
     */
    public <K, V> boolean putIfAbsent(String key, K field, V value, Duration ttl) {
        if (field == null || value == null) return false;
        Boolean set = redisTemplate.opsForHash().putIfAbsent(key, field, value);
        if (Boolean.TRUE.equals(set) && ttl != null) {
            redisTemplate.expire(key, ttl);
        }
        return Boolean.TRUE.equals(set);
    }

    // ===================== 整 hash 读写（对齐 BaseRedis 的 activeHashCache / getHashCache） =====================

    public <K, V> void putAll(String keyPrefix, Long id, Map<K, V> value, Duration ttl) {
        if (value == null || value.isEmpty() || id == null) return;
        String key = getRedisKey(keyPrefix, id);
        redisTemplate.opsForHash().putAll(key, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <K, V> void putAll(String key, Map<K, V> value, Duration ttl) {
        if (value == null || value.isEmpty()) return;
        redisTemplate.opsForHash().putAll(key, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <K, V> Map<K, V> entries(String keyPrefix, Long id) {
        return castToType(redisTemplate.opsForHash().entries(getRedisKey(keyPrefix, id)));
    }

    public <K, V> Map<K, V> entries(String key) {
        return castToType(redisTemplate.opsForHash().entries(key));
    }
}
