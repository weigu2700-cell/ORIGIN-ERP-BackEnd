package org.smart.erp.system.cache;

import org.smart.erp.common.utils.RedisUtil;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PermissionsRedis extends RedisUtil {

    private final String PERMISSIONS_KEY_PREFIX = "erp:auth:permissions";

    public PermissionsRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public PermissionCacheVo getPermissionCache(Long id) {
        return getCache(PERMISSIONS_KEY_PREFIX, id);
    }

    public void activePermissionCache(PermissionCacheVo permission) {
        activeCache(PERMISSIONS_KEY_PREFIX, permission.getId(), permission, Duration.ofHours(2));
    }

    public Boolean evictPermissionCache(Long id) {
        return evictCache(PERMISSIONS_KEY_PREFIX, id);
    }

    /**
     * 构建权限缓存对象
     * @param permission 权限实体
     * @return 权限缓存对象
     */
    public PermissionCacheVo buildPermissionCache(Permission permission) {
        PermissionCacheVo permissionCacheVo = new PermissionCacheVo();
        BeanUtils.copyProperties(permission, permissionCacheVo);
        return permissionCacheVo;
    }

}
