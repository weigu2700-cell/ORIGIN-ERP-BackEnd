package org.smart.erp.system.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.smart.erp.common.utils.RedisUtil;
import org.smart.erp.system.entity.Dashboard;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DashboardRedis extends RedisUtil {

    private final String DASHBOARD_KEY = "erp:dashboard:";

    private final StringRedisTemplate stringRedisTemplate;

    public DashboardRedis(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate
    ) {
        super(redisTemplate);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 读取看板缓存；未命中或解析失败时返回 null（由调用方回源重建）
     */
    public Dashboard getDashboardCache(Long userId) {
        if (!hasCache(DASHBOARD_KEY, userId)) {
            return null;
        }
        return getCache(DASHBOARD_KEY, userId);
    }

    /**
     * 写入看板缓存
     */
    public void activeDashboardCache(Dashboard dashboard, Long userId, Duration expire) {
        activeCache(DASHBOARD_KEY, userId, dashboard, expire);
    }

    /**
     * 失效看板缓存（生产/采购/销售订单数据变更时应调用）
     */
    public void evictDashboardCache() {
        stringRedisTemplate.delete(DASHBOARD_KEY);
    }
}
