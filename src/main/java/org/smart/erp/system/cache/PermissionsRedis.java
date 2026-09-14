package org.smart.erp.system.cache;

import org.smart.erp.common.utils.RedisUtil;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionsRedis extends RedisUtil {

    private final String PERMISSIONS_KEY_PREFIX = "erp:auth:permissions:";

    public PermissionsRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public Set<PermissionCacheVo> getPermissionsCache(Long userId) {
        return getSetCache(PERMISSIONS_KEY_PREFIX, userId);
    }

    public void activePermissionsCache(Long userId, Set<PermissionCacheVo> permissions) {
        activeSetCache(PERMISSIONS_KEY_PREFIX, userId, permissions, Duration.ofHours(2));
    }

    public void evictPermissionsCache(Long permissionId) {
        evictCache(PERMISSIONS_KEY_PREFIX, permissionId);
    }

    public Set<PermissionCacheVo> buildPermissionCache(List<Permission> permissions) {
        return permissions.stream().map(permission -> {
            PermissionCacheVo permissionCacheVo = new PermissionCacheVo();
            BeanUtils.copyProperties(permission, permissionCacheVo);
            return permissionCacheVo;
        }).collect(Collectors.toSet());
    }
}
