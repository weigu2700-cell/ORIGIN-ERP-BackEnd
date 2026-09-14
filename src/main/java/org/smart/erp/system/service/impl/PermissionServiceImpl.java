package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.system.cache.PermissionsRedis;
import org.smart.erp.system.converter.RoleConverter;
import org.smart.erp.system.dto.PermissionAddDto;
import org.smart.erp.system.dto.PermissionDetailDto;
import org.smart.erp.system.dto.PermissionUpdateDto;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.entity.RolePermission;
import org.smart.erp.system.Enum.Status;
import org.smart.erp.system.mapper.PermissionMapper;
import org.smart.erp.system.mapper.RolePermissionMapper;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.smart.erp.system.vo.PermissionTreeVo;
import org.smart.erp.system.vo.PermissionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.Role;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {


    private final CurrentUser currentUser;
    private final RoleConverter roleConverter;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionsRedis permissionsRedis;

    public PermissionServiceImpl(
            CurrentUser currentUser,
            RoleConverter roleConverter,
            RolePermissionMapper rolePermissionMapper,
            PermissionsRedis permissionsRedis
    ) {
        this.currentUser = currentUser;
        this.roleConverter = roleConverter;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionsRedis = permissionsRedis;
    }


    /**
     * 获取当前用户的权限
     * 优先从缓存中获取，如果缓存不存在，则从数据库中获取，并将数据库中的数据存入缓存中
     * @param userId 用户id
     * @return 权限集合
     * @throws BusinessException 如果缓存异常，则回退数据库，避免登录 / 鉴权链路因缓存故障直接 401 把用户踢回登录页
     */
    public Set<PermissionCacheVo> activePermissionWithCache(Long userId) {

        Set<PermissionCacheVo> permissionCacheVo = permissionsRedis.getPermissionsCache(userId);
        if (permissionCacheVo != null) {
            return permissionCacheVo;
        }

        List<Long> roleIds = roleConverter.getCurrentRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        List<Permission> permissions = this.listByIds(permissionIds);
        Set<PermissionCacheVo> permissionCacheVos = permissionsRedis.buildPermissionCache(permissions);
        permissionsRedis.activePermissionsCache(userId, permissionCacheVos);
        return permissionCacheVos;
    }

    /**
     * 将权限实体转换为 Vo。
     * parentNameById 为父级 id -> 父级名称 的映射，可由调用方批量查询后传入；
     * 不需要父级名称时传空 Map 即可（parentName 置为 null）。
     * @param p 权限实体
     * @param parentNameById 父级 id -> 父级名称 的映射
     * @return 权限 Vo
     */
    private PermissionTreeVo toVO(Permission p, Map<Long, String> parentNameById) {
        PermissionTreeVo vo = new PermissionTreeVo();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setType( p.getType());
        vo.setParentId(p.getParentId());
        vo.setParentName(p.getParentId() != null ? parentNameById.get(p.getParentId()) : null);
        vo.setSort(p.getSort());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        return vo;
    }

    // 获取当前用户的权限
    @Override
    public List<PermissionVo> detailCurrentUserPermission(Long currentUserId) {
        Set<PermissionCacheVo> permissionCacheVos = activePermissionWithCache(currentUserId);
        List<PermissionVo> vos = new ArrayList<>();
        permissionCacheVos.forEach(permissionCacheVo -> {
            PermissionVo vo = new PermissionVo();
            BeanUtils.copyProperties(permissionCacheVo, vo);
            vos.add(vo);
        });
        return vos;
    }

    @Override
    public Page<PermissionTreeVo> pagePermission(PermissionDetailDto dto) {
        LambdaQueryWrapper<Permission> queryWrapper =
                new LambdaQueryWrapper<Permission>()
                        .like(dto.getName() != null, Permission::getName, dto.getName())
                        .like(dto.getCode() != null, Permission::getCode, dto.getCode())
                        .eq(dto.getType() != null, Permission::getType, dto.getType())
                        .eq(dto.getStatus() != null, Permission::getStatus, dto.getStatus())
                        .eq(dto.getParentId() != null, Permission::getParentId, dto.getParentId())
                        .orderByDesc(Permission::getSort);

        Page<Permission> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        // 批量解析父级权限名称，避免逐条查询（N+1）
        List<Long> parentIds = page.getRecords().stream()
                .map(Permission::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> parentNameById = parentIds.isEmpty() ? Map.of() :
                this.list(new LambdaQueryWrapper<Permission>().in(Permission::getId, parentIds))
                        .stream()
                        .collect(Collectors.toMap(Permission::getId, Permission::getName));

        // 用统一转换方法把实体转成 Vo，并带上父级名称
        List<PermissionTreeVo> vos = page.getRecords().stream()
                .map(p -> toVO(p, parentNameById))
                .toList();

        Page<PermissionTreeVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    /**
     * 查询权限树（父子层级结构）。
     * 思路：
     * 1. 一次性查出所有未删除的权限（权限表通常数据量不大，全量查即可，避免递归 SQL）。
     * 2. 先全部转成 Vo，并建立 "id -> Vo" 的索引，方便子节点快速挂到父节点下。
     * 3. 遍历每个节点：
     *    - 若 parentId 为空或为 0，说明是顶级节点，放入树的根列表；
     *    - 否则从索引里找到父节点，把自己加进父节点的 children 列表。
     * 4. 按 sort 倒序（与列表接口保持一致），让前端展示有序。
     * 时间复杂度 O(n)，只查一次数据库，没有 N+1 问题。
     */
    @Override
    public List<PermissionTreeVo> getPermissionTree() {
        List<Permission> all = this.list(new LambdaQueryWrapper<Permission>().orderByDesc(Permission::getSort));

        Map<Long, PermissionTreeVo> voById = all.stream()
                .map(p -> toVO(p, Map.of()))
                .collect(Collectors.toMap(PermissionTreeVo::getId, vo -> vo));

        List<PermissionTreeVo> tree = new java.util.ArrayList<>();
        for (PermissionTreeVo node : voById.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                tree.add(node);
            } else {
                PermissionTreeVo parent = voById.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new java.util.ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @Override
    public PermissionTreeVo detailPermission(Long id) {
        Permission permission = this.getById(id);
        if (permission == null) {
            throw new BusinessException(404, "权限不存在");
        }
        Map<Long, String> parentNameById = Map.of();
        if (permission.getParentId() != null) {
            Permission parent = this.getById(permission.getParentId());
            if (parent != null) {
                parentNameById = Map.of(permission.getParentId(), parent.getName());
            }
        }
        return toVO(permission, parentNameById);
    }

    @Override
    public void addPermission(PermissionAddDto dto) {
        if (dto.getParentId() != null) {
            Permission parent = this.getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父权限不存在");
            }
        }
        if (this.count(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, dto.getCode())) > 0) {
            throw new BusinessException(400, "权限编码已存在");
        }
        Permission permission = new Permission();
        BeanUtils.copyProperties(dto, permission);
        if (permission.getStatus() == null) {
            permission.setStatus(Status.ENABLE);
        }
        this.save(permission);
        permissionsRedis.evictAllPermissionsCache();
    }

    @Override
    @Transactional
    public void updatePermission(PermissionUpdateDto dto) {
        Permission permission = this.getById(dto.getId());
        if (permission == null) {
            throw new BusinessException(404, "权限不存在");
        }
        if (dto.getParentId() != null) {
            Permission parent = this.getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父权限不存在");
            }
        }
        if (dto.getCode() != null) {
            if (!dto.getCode().equals(permission.getCode())
                    && this.count(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, dto.getCode())) > 0) {
                throw new BusinessException(400, "权限编码已存在");
            }
        }
        if (dto.getCode() != null) permission.setCode(dto.getCode());
        if (dto.getName() != null) permission.setName(dto.getName());
        if (dto.getType() != null) permission.setType(dto.getType());
        if (dto.getParentId() != null) permission.setParentId(dto.getParentId());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        if (dto.getRemark() != null) permission.setRemark(dto.getRemark());

        this.updateById(permission);
        // 权限变更影响所有用户，全量失效，避免缓存残留旧数据（TTL 2h 兜底）
        permissionsRedis.evictAllPermissionsCache();
    }

    @Override
    public List<PermissionVo> getCurrentUserPermission() {
        Long currentUserId = currentUser.getUserId();
       return detailCurrentUserPermission(currentUserId);

    }


}
