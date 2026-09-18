package org.smart.erp.system.cache;

import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.common.utils.redis.OperationSet;
import org.smart.erp.system.vo.MenuTreeVo;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class MenuRedis {

    private final String MENU_KEY_PREFIX = "erp:auth:menu:v2:";
    private final String MENU_USERS_INDEX = "erp:auth:menu:v2:users";

    private final OperationString operationString;
    private final OperationSet operationSet;

    public MenuRedis(OperationString operationString, OperationSet operationSet) {
        this.operationString = operationString;
        this.operationSet = operationSet;
    }

    public List<MenuTreeVo> getMenuCache(Long userId) {
        return operationString.get(MENU_KEY_PREFIX, userId);
    }

    public void activeMenuCache(Long userId, List<MenuTreeVo> menus) {
        operationString.set(MENU_KEY_PREFIX, userId, menus, Duration.ofHours(2));
        operationSet.add(MENU_USERS_INDEX, userId, Duration.ofHours(2));
    }

    public void evictMenuCache(Long userId) {
        operationString.delete(MENU_KEY_PREFIX, userId);
        operationSet.remove(MENU_USERS_INDEX, userId);
    }

    public void evictAllMenuCache() {
        Set<Object> userIds = operationSet.members(MENU_USERS_INDEX);
        if (userIds != null) {
            for (Object rawId : userIds) {
                Long userId = toLong(rawId);
                if (userId != null) {
                    operationString.delete(MENU_KEY_PREFIX, userId);
                }
            }
        }
        operationSet.delete(MENU_USERS_INDEX);
    }

    /**
     * 把 Redis 取出的成员安全转成 Long。
     * 正常情况下成员是 Long；若因历史数据/旧序列化器残留为 String，也能正确解析，
     * 避免 {@code for (Long id : ...)} 在运行时把 String 当 Long 强转抛出 ClassCastException。
     */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
