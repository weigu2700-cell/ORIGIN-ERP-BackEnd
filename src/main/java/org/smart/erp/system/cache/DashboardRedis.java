package org.smart.erp.system.cache;

import org.smart.erp.common.utils.BaseRedis;
import org.smart.erp.system.vo.DashboardVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DashboardRedis extends BaseRedis {

    private static final String DASHBOARD_KEY = "erp:dashboard";

    private static final Duration TTL = Duration.ofSeconds(60);

    public DashboardRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    /**
     * 读取全局看板缓存；未命中或解析失败时返回 null（由调用方回源重建）
     */
    public DashboardVo getDashboardCache() {
        if (!hasKey(DASHBOARD_KEY)) {
            return null;
        }
        return getCache(DASHBOARD_KEY);
    }

    /**
     * 写入全局看板缓存（固定 60 秒 TTL）
     */
    public void activeDashboardCache(DashboardVo dashboardVo) {
        activeCache(DASHBOARD_KEY, dashboardVo, TTL);
    }
}
