package org.smart.erp.system.cache;

import org.smart.erp.common.utils.RandomTtl;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.system.vo.DashboardVo;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DashboardRedis {

    private static final String DASHBOARD_KEY = "erp:dashboard:";
    private static final String DASHBOARD_LOCK_KEY = "erp:lock:dashboard:";
    private static final Duration TTL = RandomTtl.ofMinutes(1, 2);

    private final OperationString operationString;

    public DashboardRedis(OperationString operationString) {
        this.operationString = operationString;
    }

    public String getDashboardLockKey() {
        return DASHBOARD_LOCK_KEY;
    }

    /**
     * 读取全局看板缓存；未命中或解析失败时返回 null（由调用方回源重建）
     */
    public DashboardVo getDashboardCache() {
        if (!operationString.hasKey(DASHBOARD_KEY)) {
            return null;
        }
        return operationString.get(DASHBOARD_KEY);
    }

    /**
     * 写入全局看板缓存（随机 60~120 秒 TTL，避免多实例同时过期击穿）
     */
    public void activeDashboardCache(DashboardVo dashboardVo) {
        operationString.set(DASHBOARD_KEY, dashboardVo, TTL);
    }
}
