package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.security.CurrentUserImpl;
import org.smart.erp.system.converter.RoleConverter;
import org.smart.erp.system.dto.MenuCreateDTO;
import org.smart.erp.system.dto.MenuGetDTO;
import org.smart.erp.system.dto.MenuGetTreeDTO;
import org.smart.erp.system.entity.Menu;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.entity.RoleMenu;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.mapper.MenuMapper;
import org.smart.erp.system.mapper.RoleInfoMapper;
import org.smart.erp.system.mapper.RoleMenuMapper;
import org.smart.erp.system.mapper.UserRoleMapper;
import org.smart.erp.system.service.MenuService;
import org.smart.erp.system.vo.MenuListVO;
import org.smart.erp.system.vo.MenuTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final CurrentUser currentUser;
    private final UserRoleMapper userRoleMapper;
    private final RoleConverter roleConverter;
    private final RoleInfoMapper roleInfoMapper;

    public MenuServiceImpl(
            MenuMapper menuMapper ,
            RoleMenuMapper roleMenuMapper ,
            CurrentUser currentUser,
            UserRoleMapper userRoleMapper,
            RoleConverter roleConverter,
            RoleInfoMapper roleInfoMapper
    )
    {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.currentUser = currentUser;
        this.userRoleMapper = userRoleMapper;
        this.roleConverter = roleConverter;
        this.roleInfoMapper = roleInfoMapper;
    }

    /** 判断角色列表中是否包含超级管理员（角色编码为 admin） */
    private boolean isSuperAdmin(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return false;
        }
        List<RoleInfo> roles = roleInfoMapper.selectList(new LambdaQueryWrapper<RoleInfo>()
                .in(RoleInfo::getId, roleIds));
        return roles.stream().anyMatch(role -> "admin".equals(role.getCode()));
    }

    private MenuListVO toVO(Menu menu, Map<Long, String> parentNameById) {
        MenuListVO vo = new MenuListVO();
        BeanUtils.copyProperties(menu, vo);
        vo.setParentName(menu.getParentId() != null ? parentNameById.get(menu.getParentId()) : null);
        return vo;
    }

    /** 仅做实体 -> 树VO 的字段拷贝，不负责挂 children（由 getMenuTree 统一组装） */
    private MenuTreeVO toTreeVO(Menu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    /** 获取菜单树形结构*/
    @NonNull
    private List<MenuTreeVO> getMenuTreeVOS(List<Long> menuIds) {
        // 未分配任何菜单时直接返回空树，避免空条件导致查全表
        if (CollectionUtils.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .in(Menu::getId, menuIds));

        Map<Long, MenuTreeVO> voById = menus.stream()
                .collect(Collectors.toMap(Menu::getId, this::toTreeVO));

        List<MenuTreeVO> tree = new ArrayList<>();

        for (MenuTreeVO node : voById.values()) {
            Long parentId = node.getParentId();
            if (parentId == null) {
                tree.add(node);
            } else {
                MenuTreeVO parent = voById.get(parentId);
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
    public Page<MenuListVO> listMenu(MenuGetDTO dto) {
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

        List<MenuListVO> voList = page.getRecords().stream()
                .map(menu -> toVO(menu, parentNameById))
                .collect(Collectors.toList());

        Page<MenuListVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;

    }

    @Override
    public List<MenuTreeVO> getMenuTree(MenuGetTreeDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException(400, "roleId 不能为空");
        }
        List<RoleMenu> roleMenuList = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, dto.getRoleId()));

        List<Long> menuIds = roleMenuList.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());

        return getMenuTreeVOS(menuIds);
    }

    @Override
    public MenuListVO getMenuDetail(Long id) {
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
    public void createMenu(MenuCreateDTO dto) {
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu);
        this.save(menu);
    }

    @Override
    public void updateMenu(Long id, MenuCreateDTO dto) {
       Menu menu = this.getById(id);
       if (menu == null) {
           throw new BusinessException(404, "菜单不存在");
       }
       if (dto.getName() != null) menu.setName(dto.getName());
       if (dto.getTitle() != null) menu.setTitle(dto.getTitle());
       if (dto.getPath() != null) menu.setPath(dto.getPath());
       if (dto.getComponent() != null) menu.setComponent(dto.getComponent());
       if (dto.getIcon() != null) menu.setIcon(dto.getIcon());
       if (dto.getParentId() != null) menu.setParentId(dto.getParentId());
       if (dto.getVisible() != null) menu.setVisible(dto.getVisible());
       if (dto.getStatus() != null) menu.setStatus(dto.getStatus());

       this.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
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
    }

    @Override
    public List<MenuTreeVO> getCurrentUserMenu() {

        Long currentUserId = currentUser.getUserId();
        List<Long> roleIds = roleConverter.getCurrentRoleIds(currentUserId);

        // 超级管理员默认拥有全部菜单
        if (isSuperAdmin(roleIds)) {
            List<Long> allMenuIds = menuMapper.selectList(new LambdaQueryWrapper<Menu>())
                    .stream()
                    .map(Menu::getId)
                    .toList();
            return getMenuTreeVOS(allMenuIds);
        }

        // 用户没有任何角色时，不应看到任何菜单
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        List<RoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                .in(RoleMenu::getRoleId, roleIds));

        List<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .toList();

        return getMenuTreeVOS(menuIds);

    }

}
