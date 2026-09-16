package org.smart.erp.system.cache;

import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.system.vo.MenuTreeVo;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class MenuRedis {

    private final String MENU_KEY_PREFIX = "erp:auth:menu:";

    private final OperationString operationString;

    public MenuRedis(OperationString operationString) {
        this.operationString = operationString;
    }

    public List<MenuTreeVo> getMenuCache(Long userId) {
        return operationString.get(MENU_KEY_PREFIX, userId);
    }

    public void activeMenuCache(Long userId, List<MenuTreeVo> menus) {
        operationString.set(MENU_KEY_PREFIX, userId, menus, Duration.ofHours(2));
    }

    public void evictMenuCache(Long userId) {
        operationString.delete(MENU_KEY_PREFIX, userId);
    }
}
