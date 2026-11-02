package org.smart.erp.tenant.context;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** 统一覆盖值、集合索引、Lua KEYS 等入口；hash field/value 不加前缀。 */
public final class TenantRedisKeySerializer implements RedisSerializer<String> {
    private final TenantKeyspace keys;
    public TenantRedisKeySerializer(TenantKeyspace keys) { this.keys = keys; }
    @Override public byte[] serialize(String value) {
        return value == null ? null : StringRedisSerializer.UTF_8.serialize(keys.key(value));
    }
    @Override public String deserialize(byte[] value) {
        return keys.unmap(StringRedisSerializer.UTF_8.deserialize(value));
    }
}
