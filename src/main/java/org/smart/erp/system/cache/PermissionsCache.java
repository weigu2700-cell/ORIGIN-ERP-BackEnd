package org.smart.erp.system.cache;

import org.smart.erp.common.utils.RedisUtil;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.smart.erp.system.vo.PermissionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PermissionsCache extends RedisUtil {

    private final String PERMISSIONS_KEY = "erp:auth:permissions";

    public PermissionsCache(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public PermissionCacheVo getPermissionCache(Long id) {
        return getCache(PERMISSIONS_KEY, id);
    }

    public void activePermissionCache(Permission permission) {
        activeCache(
                PERMISSIONS_KEY,
                permission.getId(),
                buildPermissionCache(permission),
                Duration.ofHours(1)
        );
    }

    public Boolean evictPermissionCache(Long id) {
        return evictCache(PERMISSIONS_KEY, id);
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
