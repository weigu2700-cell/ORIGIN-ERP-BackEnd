package org.smart.erp.im;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** 显式企业键；握手消费票据时还没有租户上下文，不能使用隐式前缀模板。 */
@Component
public class ImRedis {

    public static final String CHANNEL = "erp:im:wakeup:v1";

    private final StringRedisTemplate template;

    public ImRedis(RedisConnectionFactory factory) {
        template = new StringRedisTemplate(factory);
    }

    public String key(String tenant, String suffix) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,64}"))
            throw ImException.invalid("企业身份无效");
        return "erp:im:t:" + tenant + ":" + suffix;
    }

    public void put(String key, String value, Duration ttl) {
        template.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return template.opsForValue().get(key);
    }

    public String consume(String key) {
        return template.opsForValue().getAndDelete(key);
    }

    public void publish(String value) {
        template.convertAndSend(CHANNEL, value);
    }

    public void rate(String tenant, long user, String operation, int limit) {
        var script = new DefaultRedisScript<>(
                "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],1) end; return n",
                Long.class);
        Long count = template.execute(script, List.of(key(tenant, "rate:" + operation + ":" + user)));
        if (count == null || count > Math.max(1, limit))
            throw new ImException(429, "RATE_LIMITED", "操作过于频繁，请稍后再试");
    }

    public boolean claimConnection(String tenant, long user, String client, String connection, int maximum) {
        String member = client + ":" + connection;
        var script = new DefaultRedisScript<>("""
                local now=tonumber(ARGV[1]); redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',now)
                local members=redis.call('ZRANGE',KEYS[1],0,-1)
                for _,m in ipairs(members) do
                  if string.sub(m,1,string.len(ARGV[3])+1)==ARGV[3]..':' then redis.call('ZREM',KEYS[1],m) end
                end
                if redis.call('ZCARD',KEYS[1])>=tonumber(ARGV[4]) then return 0 end
                redis.call('ZADD',KEYS[1],now+90000,ARGV[2]); redis.call('PEXPIRE',KEYS[1],120000); return 1
                """, Long.class);
        Long result = template.execute(script, List.of(key(tenant, "connections:" + user)),
                Long.toString(System.currentTimeMillis()), member, client, Integer.toString(Math.clamp(maximum, 1, 5)));
        return Long.valueOf(1).equals(result);
    }

    public boolean renewConnection(String tenant, long user, String client, String connection) {
        var script = new DefaultRedisScript<>(
                """
                        local score=redis.call('ZSCORE',KEYS[1],ARGV[1]); if not score or tonumber(score)<=tonumber(ARGV[2]) then return 0 end
                        redis.call('ZADD',KEYS[1],'XX',tonumber(ARGV[2])+90000,ARGV[1]); redis.call('PEXPIRE',KEYS[1],120000); return 1
                        """,
                Long.class);
        return Long.valueOf(1)
            .equals(template.execute(script, List.of(key(tenant, "connections:" + user)), client + ":" + connection,
                    Long.toString(System.currentTimeMillis())));
    }

    public void releaseConnection(String tenant, long user, String client, String connection) {
        template.opsForZSet().remove(key(tenant, "connections:" + user), client + ":" + connection);
    }

}
