package org.smart.erp.common.utils.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

public abstract class OperationRedis {

    protected final RedisTemplate<String, Object> redisTemplate;

    protected OperationRedis(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected String getRedisKey(String cacheKey, Long id) {
        return cacheKey + id;
    }

    @SuppressWarnings("unchecked")
    protected static <T> T castToType(Object value) {
        return (T) value;
    }

    /**
     * 通用 Lua 脚本原子执行入口（下沉到 Redis 公共工具层）。
     * KEYS/ARGV 的语义与传递方式遵循 Redis EVAL 约定：脚本中 KEYS[i] 对应 keys 第 i 个元素，
     * ARGV[i] 对应 args 第 i 个元素。典型用途：原子校验 + 删除分布式锁、原子计数、限流等。
     *
     * @param script Redis 脚本对象（建议用 RedisScript.of(lua, resultType) 构造）
     * @param keys   KEYS 列表（对应脚本中的 KEYS[i]）
     * @param args   ARGV 参数（对应脚本中的 ARGV[i]，变长）
     * @param <T>    脚本返回类型
     * @return 脚本执行结果
     */
    public <T> T executeScript(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

}
