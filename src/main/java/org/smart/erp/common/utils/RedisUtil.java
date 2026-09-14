package org.smart.erp.common.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取Redis Key
     * @param cacheKey 缓存Key
     * @param id 实体ID
     * @return Redis Key
     */
    public String getRedisKey(String cacheKey, Long id) {
        return cacheKey + id;
    }

    /**
     * 获取缓存
     * @param cacheKey 缓存Key
     * @param id 实体ID
     * @return 缓存对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 激活缓存
     * @param cacheKey 缓存Key
     * @param id 实体ID
     * @param value 缓存对象
     * @param ttl 过期时间
     */
    public <T> void activeCache(String cacheKey, Long id, T value, Duration ttl) {
        if (value == null || id == null) return;
        String key = getRedisKey(cacheKey, id);
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 失效缓存
     * @param cacheKey 缓存Key
     * @param id 实体ID
     * @return 是否失效成功
     */
    public Boolean evictCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);
        redisTemplate.delete(key);
        return true;
    }
}
