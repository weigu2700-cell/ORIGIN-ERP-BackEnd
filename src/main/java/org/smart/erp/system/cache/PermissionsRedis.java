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
    // 记录已缓存权限的用户，供权限变更时批量失效（权限缓存以 userId 为 key，无法凭 permissionId 反查用户）
    private final String PERMISSIONS_USERS_INDEX = "erp:auth:permissions:users";

    private final RedisTemplate<String, Object> redisTemplate;

    public PermissionsRedis(
            RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
        this.redisTemplate = redisTemplate;
    }

    public Set<PermissionCacheVo> getPermissionsCache(Long userId) {
        if (!hasCache(PERMISSIONS_KEY_PREFIX, userId)) {
            return null;
        }
        return getSetCache(PERMISSIONS_KEY_PREFIX, userId);
    }

    public void activePermissionsCache(Long userId, Set<PermissionCacheVo> permissions) {
        activeSetCache(PERMISSIONS_KEY_PREFIX, userId, permissions, Duration.ofHours(2));
        redisTemplate.opsForSet().add(PERMISSIONS_USERS_INDEX, userId);
        redisTemplate.expire(PERMISSIONS_USERS_INDEX, Duration.ofHours(2));
    }

    /**
     * 失效指定用户的权限缓存（如用户角色关系变更时调用）
     */
    public void evictPermissionsCache(Long userId) {
        evictCache(PERMISSIONS_KEY_PREFIX, userId);
        redisTemplate.opsForSet().remove(PERMISSIONS_USERS_INDEX, userId);
    }

    /**
     * 权限新增 / 修改时调用：失效所有已缓存的用户权限。
     * 因权限缓存以 userId 为 key，无法仅凭 permissionId 定位受影响用户，故采用全量失效。
     * 该操作为低频管理动作，可接受全量开销。
     */
    public void evictAllPermissionsCache() {
        Set<Object> userIds = redisTemplate.opsForSet().members(PERMISSIONS_USERS_INDEX);
        if (userIds != null) {
            for (Object id : userIds) {
                evictCache(PERMISSIONS_KEY_PREFIX, ((Number) id).longValue());
            }
        }
        redisTemplate.delete(PERMISSIONS_USERS_INDEX);
    }

    public Set<PermissionCacheVo> buildPermissionCache(List<Permission> permissions) {
        return permissions.stream().map(permission -> {
            PermissionCacheVo permissionCacheVo = new PermissionCacheVo();
            BeanUtils.copyProperties(permission, permissionCacheVo);
            return permissionCacheVo;
        }).collect(Collectors.toSet());
    }
}
