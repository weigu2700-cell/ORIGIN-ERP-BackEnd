package org.smart.erp.system.cache;

import org.smart.erp.common.utils.RedisUtil;
import org.smart.erp.system.vo.MenuTreeVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class MenuRedis extends RedisUtil {

    private final String MENU_KEY_PREFIX = "erp:auth:menu:";

    public MenuRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public List<MenuTreeVo> getMenuCache(Long userId) {
        return getCache(MENU_KEY_PREFIX, userId);
    }

    public void activeMenuCache(Long userId, List<MenuTreeVo> menus) {
        activeCache(MENU_KEY_PREFIX, userId, menus, Duration.ofHours(2));
    }

    public void evictMenuCache(Long userId) {
        evictCache(MENU_KEY_PREFIX, userId);
    }
}
