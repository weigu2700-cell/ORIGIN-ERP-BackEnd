package org.smart.erp.common.utils.redis;


public abstract class OperationRedis {

    public String getRedisKey(String cacheKey, Long id) {
        return cacheKey + id;
    }

    @SuppressWarnings("unchecked")
    protected static <T> T castToType(Object value) {
        return (T) value;
    }
}
