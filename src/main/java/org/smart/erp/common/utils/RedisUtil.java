package org.smart.erp.common.utils;

import org.smart.erp.system.vo.PermissionCacheVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 获取缓存Set
     * @param cacheKey 缓存key
     * @param id 实体ID
     * @return 缓存Set对象
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSetCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);

        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                .map(member -> (T) member)
                .collect(Collectors.toSet());
    }

    /**
     * 获取缓存ZSet
     * @param cacheKey 缓存key
     * @param id 实体ID
     * @return 缓存ZSet对象
     * @param <T> 值类型
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getZSetCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);

        // 读取整个 ZSet（按分数升序、索引 0 到 -1 即全部成员）；distinctRandomMembers(key,1) 只会随机返回 1 个成员
        Set<Object> members = redisTemplate.opsForZSet().range(key, 0, -1);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                .map(member -> (T) member)
                .collect(Collectors.toSet());
    }

    /**
     * 获取缓存Hash
     * @param cacheKey 缓存key
     * @param id 实体对象Id
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存Hash对象
     */
    @SuppressWarnings("unchecked")
    public <K,V> Map<K,V> getHashCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);
        // entries() 实际返回 LinkedHashMap，不能强转为 HashMap，否则运行时 ClassCastException
        return (Map<K, V>) redisTemplate.opsForHash().entries(key);
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
     * 激活Set缓存
     *
     * @param cacheKey 缓存key
     * @param id       实体对象Id
     * @param values   缓存对象
     * @param ttl      过期时间
     * @return
     */
    public <T> Set<PermissionCacheVo> activeSetCache(String cacheKey, Long id, Set<T> values, Duration ttl) {
        if (values == null || id == null) return null;
        String key = getRedisKey(cacheKey, id);
        // add 是变参方法，需把 Set 展开为元素逐个写入；过期时间单独设置
        redisTemplate.opsForSet().add(key, values.toArray());
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
        return null;
    }

    /**
     * 激活ZSet缓存
     * @param cacheKey 缓存key
     * @param id 实体对象Id
     * @param values 缓存对象
     * @param ttl 过期时间
     */
    public <T> void activeZSetCache(String cacheKey, Long id, Set<T> values, Duration ttl) {
        if (values == null || id == null) return;
        String key = getRedisKey(cacheKey, id);
        redisTemplate.opsForZSet().add(key, values.toArray(), 1.0);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    /**
     * 激活Hash缓存
     * @param cacheKey 缓存key
     * @param id 实体对象Id
     * @param value 缓存对象（整张 Map）
     * @param ttl 过期时间
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public <K,V> void activeHashCache(String cacheKey, Long id, Map<K,V> value, Duration ttl) {
        if (value == null || id == null) return;
        String key = getRedisKey(cacheKey, id);
        // HashOperations.put 签名为 put(key, hashKey, value)，整张 Map 写入用 putAll，过期时间单独设置
        redisTemplate.opsForHash().putAll(key, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    /**
     * 失效缓存
     * @param cacheKey 缓存Key
     * @param id 实体ID
     */
    public void evictCache(String cacheKey, Long id) {
        String key = getRedisKey(cacheKey, id);
        redisTemplate.delete(key);
    }
}
