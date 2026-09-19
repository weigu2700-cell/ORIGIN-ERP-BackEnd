package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.system.cache.MenuRedis;
import org.smart.erp.system.converter.RoleConverter;
import org.smart.erp.system.dto.MenuAddDto;
import org.smart.erp.system.dto.MenuDetailDto;
import org.smart.erp.system.dto.MenuTreeDto;
import org.smart.erp.system.entity.Menu;
import org.smart.erp.system.entity.RoleMenu;
import org.smart.erp.system.Enum.Status;
import org.smart.erp.system.mapper.MenuMapper;
import org.smart.erp.system.mapper.RoleMenuMapper;
import org.smart.erp.system.service.MenuService;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.MenuListVo;
import org.smart.erp.system.vo.MenuSearchVo;
import org.smart.erp.system.vo.MenuTreeVo;
import org.smart.erp.system.vo.PermissionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final CurrentUser currentUser;
    private final RoleConverter roleConverter;
    private final MenuRedis menuRedis;
    private final PermissionService permissionService;

    public MenuServiceImpl(
            MenuMapper menuMapper ,
            RoleMenuMapper roleMenuMapper ,
            CurrentUser currentUser,
            RoleConverter roleConverter,
            MenuRedis menuRedis,
            PermissionService permissionService
    )
    {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.currentUser = currentUser;
        this.roleConverter = roleConverter;
        this.menuRedis = menuRedis;
        this.permissionService = permissionService;
    }

    private List<MenuTreeVo> activeMenuWithCache(Long userId) {
        try {
            List<MenuTreeVo> menuTree = menuRedis.getMenuCache(userId);
            if (menuTree != null) {
                return menuTree;
            }
            menuTree = buildCurrentUserMenu(userId);
            menuRedis.activeMenuCache(userId, menuTree);
            return menuTree;
        } catch (Exception ex) {
            // Redis 不可用等缓存故障时回退数据库，保证菜单接口不依赖 Redis 存活
            return buildCurrentUserMenu(userId);
        }
    }


    private MenuListVo toVO(Menu menu, Map<Long, String> parentNameById) {
        MenuListVo vo = new MenuListVo();
        BeanUtils.copyProperties(menu, vo);
        vo.setParentName(menu.getParentId() != null ? parentNameById.get(menu.getParentId()) : null);
        return vo;
    }

    /** 仅做实体 -> 树VO 的字段拷贝，不负责挂 children（由 getMenuTree 统一组装） */
    private MenuTreeVo toTreeVO(Menu menu) {
        MenuTreeVo vo = new MenuTreeVo();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    /** 获取菜单树形结构*/
    @NonNull
    private List<MenuTreeVo> getMenuTreeVoS(List<Long> menuIds) {
        // 未分配任何菜单时直接返回空树，避免空条件导致查全表
        if (CollectionUtils.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .in(Menu::getId, menuIds));

        Map<Long, MenuTreeVo> voById = menus.stream()
                .collect(Collectors.toMap(Menu::getId, this::toTreeVO));

        List<MenuTreeVo> tree = new ArrayList<>();

        for (MenuTreeVo node : voById.values()) {
            Long parentId = node.getParentId();
            if (parentId == null) {
                tree.add(node);
            } else {
                MenuTreeVo parent = voById.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
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
    public Page<MenuListVo> pageMenu(MenuDetailDto dto) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(dto.getName() != null, Menu::getName, dto.getName());
        queryWrapper.like(dto.getTitle() != null, Menu::getTitle, dto.getTitle());
        queryWrapper.eq(dto.getStatus() != null, Menu::getStatus, dto.getStatus());
        queryWrapper.eq(dto.getParentId() != null, Menu::getParentId, dto.getParentId());

        int current = dto.getPage() != null && dto.getPage() > 0 ? dto.getPage() : 1;
        int size = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
        Page<Menu> page = this.page(new Page<>(current, size), queryWrapper);

        List<Long> parentIds = page.getRecords().stream()
                .map(Menu::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Menu> parentList = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>()
                        .in(CollectionUtils.isNotEmpty(parentIds), Menu::getId, parentIds)
        );
        Map<Long, String> parentNameById = parentList.stream()
                .collect(Collectors.toMap(Menu::getId, Menu::getName));

        List<MenuListVo> voList = page.getRecords().stream()
                .map(menu -> toVO(menu, parentNameById))
                .collect(Collectors.toList());

        Page<MenuListVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;

    }

    @Override
    public List<MenuTreeVo> getMenuTree(MenuTreeDto dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException(400, "roleId 不能为空");
        }
        List<RoleMenu> roleMenuList = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, dto.getRoleId()));

        List<Long> menuIds = roleMenuList.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());

        return getMenuTreeVoS(menuIds);
    }

    @Override
    public MenuListVo detailMenu(Long id) {
        Menu menu = this.getById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        Map<Long, String> parentNameById = Map.of();
        if (menu.getParentId() != null) {
            Menu parent = menuMapper.selectById(menu.getParentId());
            if (parent != null) {
                parentNameById = Map.of(menu.getParentId(), parent.getName());
            }
        }
        return toVO(menu, parentNameById);
    }

    @Override
    public void addMenu(MenuAddDto dto) {
        if (dto.getName() != null) {
            long nameCount = this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getName, dto.getName()));
            if (nameCount > 0) {
                throw new BusinessException(400, "菜单名称已存在: " + dto.getName());
            }
        }
        if (dto.getPath() != null) {
            long pathCount = this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getPath, dto.getPath()));
            if (pathCount > 0) {
                throw new BusinessException(400, "菜单路径已存在: " + dto.getPath());
            }
        }
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu);
        if (menu.getStatus() == null) {
            menu.setStatus(Status.ENABLE);
        }
        this.save(menu);
        menuRedis.evictAllMenuCache();
    }

    @Override
    public void updateMenu(Long id, MenuAddDto dto) {
       Menu menu = this.getById(id);
       if (menu == null) {
           throw new BusinessException(404, "菜单不存在");
       }
       if (dto.getName() != null) menu.setName(dto.getName());
       if (dto.getTitle() != null) menu.setTitle(dto.getTitle());
       if (dto.getPath() != null) menu.setPath(dto.getPath());
       if (dto.getComponent() != null) menu.setComponent(dto.getComponent());
       if (dto.getIcon() != null) menu.setIcon(dto.getIcon());
       if (dto.getPermissionCode() != null) menu.setPermissionCode(dto.getPermissionCode());
       if (dto.getParentId() != null) menu.setParentId(dto.getParentId());
       if (dto.getVisible() != null) menu.setVisible(dto.getVisible());
       if (dto.getStatus() != null) menu.setStatus(dto.getStatus());

       this.updateById(menu);
       menuRedis.evictAllMenuCache();
    }

    @Override
    public void removeMenu(Long id) {
        Menu menu = this.getById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getMenuId, id));
        if (!roleMenus.isEmpty()) {
            throw new BusinessException(400, "菜单已被角色使用，无法删除");
        }
        this.removeById(id);
        menuRedis.evictAllMenuCache();
    }

    @Override
    public List<MenuTreeVo> getCurrentUserMenu() {
        return activeMenuWithCache(currentUser.getUserId());
    }

    /** 按用户角色构建菜单树（缓存未命中或缓存故障时的数据源） */
    private List<MenuTreeVo> buildCurrentUserMenu(Long currentUserId) {
        List<Long> roleIds = roleConverter.getCurrentRoleIds(currentUserId);
        boolean superAdmin = roleConverter.isSuperAdmin(currentUserId);

        // 用户没有任何角色时，不应看到任何菜单
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        Set<Long> menuIds = superAdmin
                ? menuMapper.selectList(new LambdaQueryWrapper<Menu>()).stream().map(Menu::getId).collect(Collectors.toSet())
                : roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)).stream()
                        .map(RoleMenu::getMenuId).collect(Collectors.toSet());
        if (menuIds.isEmpty()) return new ArrayList<>();

        Set<String> permissionCodes = permissionService.detailCurrentUserPermission(currentUserId).stream()
                .map(PermissionVo::getCode).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> visibleMenuIds = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                        .in(Menu::getId, menuIds)
                        .eq(Menu::getStatus, Status.ENABLE)
                        .eq(Menu::getVisible, 1))
                .stream()
                .filter(menu -> menu.getPermissionCode() == null || menu.getPermissionCode().isBlank()
                        || permissionCodes.contains(menu.getPermissionCode()))
                .map(Menu::getId)
                .toList();
        return getMenuTreeVoS(visibleMenuIds);
    }

    @Override
    public List<MenuSearchVo> searchCurrentUserMenu(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        Long currentUserId = currentUser.getUserId();
        List<Long> roleIds = roleConverter.getCurrentRoleIds(currentUserId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }

        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, Status.ENABLE)
                .eq(Menu::getVisible, 1)
                .apply("deleted = 0")
                .orderByAsc(Menu::getId);

        if (!roleConverter.isSuperAdmin(currentUserId)) {
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                            .in(RoleMenu::getRoleId, roleIds))
                    .stream()
                    .map(RoleMenu::getMenuId)
                    .distinct()
                    .toList();
            if (CollectionUtils.isEmpty(menuIds)) {
                return List.of();
            }
            queryWrapper.in(Menu::getId, menuIds);
        }

        Set<String> permissionCodes = permissionService.detailCurrentUserPermission(currentUserId).stream()
                .map(permission -> permission.getCode()).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Menu> accessibleMenus = menuMapper.selectList(queryWrapper).stream()
                .filter(menu -> menu.getPermissionCode() == null || menu.getPermissionCode().isBlank()
                        || permissionCodes.contains(menu.getPermissionCode()))
                .toList();
        Set<Long> parentIds = accessibleMenus.stream()
                .map(Menu::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Menu> menuById = accessibleMenus.stream()
                .collect(Collectors.toMap(Menu::getId, menu -> menu));

        return accessibleMenus.stream()
                .filter(menu -> !parentIds.contains(menu.getId()))
                .filter(menu -> matchesKeyword(menu, normalizedKeyword, menuById))
                .limit(20)
                .map(menu -> toSearchVO(menu, menuById))
                .toList();
    }

    private boolean matchesKeyword(Menu menu, String keyword, Map<Long, Menu> menuById) {
        Menu current = menu;
        while (current != null) {
            if (containsIgnoreCase(current.getTitle(), keyword)
                    || containsIgnoreCase(current.getName(), keyword)) {
                return true;
            }
            current = current.getParentId() == null ? null : menuById.get(current.getParentId());
        }
        return false;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private MenuSearchVo toSearchVO(Menu menu, Map<Long, Menu> menuById) {
        MenuSearchVo vo = new MenuSearchVo();
        vo.setId(menu.getId());
        vo.setTitle(menu.getTitle());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        Menu parent = menu.getParentId() == null ? null : menuById.get(menu.getParentId());
        vo.setParentTitle(parent == null ? null : parent.getTitle());
        return vo;
    }

}
