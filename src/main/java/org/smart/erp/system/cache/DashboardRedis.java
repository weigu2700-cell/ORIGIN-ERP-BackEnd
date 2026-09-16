package org.smart.erp.system.cache;

import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.system.vo.DashboardVo;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DashboardRedis {

    private static final String DASHBOARD_KEY = "erp:dashboard";

    private static final Duration TTL = Duration.ofSeconds(60);

    private final OperationString operationString;

    public DashboardRedis(OperationString operationString) {
        this.operationString = operationString;
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
     * 写入全局看板缓存（固定 60 秒 TTL）
     */
    public void activeDashboardCache(DashboardVo dashboardVo) {
        operationString.set(DASHBOARD_KEY, dashboardVo, TTL);
    }
}
