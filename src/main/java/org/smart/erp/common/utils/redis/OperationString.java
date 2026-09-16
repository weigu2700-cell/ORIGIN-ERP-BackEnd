package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 值（String/Object）类型操作工具类，对应 opsForValue()。
 * 提供两套 API：
 *  1) 按 cacheKey + id 的实体维度缓存（自动拼接 key）；
 *  2) 基于完整 key 的全局/自定义缓存。
 * 两套方法通过方法重载区分，调用时按参数个数自然消歧。
 */
@Component
public class OperationString extends OperationRedis {

    public OperationString(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    // ===================== 按 cacheKey + id 的实体维度缓存 =====================

    /**
     * 写入缓存（原子设置 TTL；ttl 为 null 表示永久）
     * @param keyPrefix 缓存key前缀
     * @param id 实体ID
     * @param value 缓存值
     * @param ttl 过期时间，null 表示不过期
     * @param <T> 缓存值类型
     */
    public <T> void set(String keyPrefix, Long id, T value, Duration ttl) {
        if (value == null || id == null) return;
        String key = getRedisKey(keyPrefix, id);
        if (ttl != null) {
            redisTemplate.opsForValue().set(key, value, ttl);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 读取缓存；未命中返回 null
     * @param keyPrefix 缓存key前缀
     * @param id 实体ID
     * @param <T> 缓存值类型
     * @return 缓存对象，未命中返回 null
     */
    public <T> T get(String keyPrefix, Long id) {
        Object value = redisTemplate.opsForValue().get(getRedisKey(keyPrefix, id));
        return value == null ? null : castToType(value);
    }

    /**
     * 判断缓存 key 是否存在（未写入 / 已过期均视为不存在）
     * @param keyPrefix 缓存key前缀
     * @param id 实体ID
     * @return 是否存在
     */
    public boolean hasKey(String keyPrefix, Long id) {
        return redisTemplate.hasKey(getRedisKey(keyPrefix, id));
    }

    /**
     * 删除缓存
     * @param keyPrefix 缓存key前缀
     * @param id 实体ID
     */
    public void delete(String keyPrefix, Long id) {
        redisTemplate.delete(getRedisKey(keyPrefix, id));
    }

    /**
     * NX 写入：key 不存在时才写入，返回是否写入成功（用于防并发重复操作）。
     * 操作结束（成功或异常）调用方需自行 delete 释放，TTL 仅作崩溃兜底。
     * @param keyPrefix 缓存key前缀
     * @param id 实体ID
     * @param value 写入值（如当前操作人ID）
     * @param ttl 过期时间
     * @param <T> 值类型
     * @return true=本次成功写入（此前不存在），false=已存在未覆盖
     */
    public <T> boolean setIfAbsent(String keyPrefix, Long id, T value, Duration ttl) {
        if (value == null || id == null) return false;
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(getRedisKey(keyPrefix, id), value, ttl));
    }

    // ===================== 基于完整 key 的全局/自定义缓存 =====================

    /**
     * 写入缓存（原子设置 TTL；ttl 为 null 表示永久）
     * @param key 完整 Redis key
     * @param value 缓存值
     * @param ttl 过期时间，null 表示不过期
     * @param <T> 缓存值类型
     */
    public <T> void set(String key, T value, Duration ttl) {
        if (value == null) return;
        if (ttl != null) {
            redisTemplate.opsForValue().set(key, value, ttl);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 读取缓存（基于完整 key）；未命中返回 null
     * @param key 完整 Redis key
     * @param <T> 缓存值类型
     * @return 缓存对象，未命中返回 null
     */
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : castToType(value);
    }

    /**
     * 判断缓存 key 是否存在（基于完整 key）
     * @param key 完整 Redis key
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除缓存（基于完整 key）
     * @param key 完整 Redis key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * NX 写入（基于完整 key）：key 不存在时才写入，返回是否写入成功。
     * @param key 完整 Redis key
     * @param value 写入值
     * @param ttl 过期时间
     * @param <T> 值类型
     * @return true=本次成功写入（此前不存在），false=已存在未覆盖
     */
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        if (value == null) return false;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttl));
    }


}
