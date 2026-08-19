package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.system.dto.RoleGetDTO;
import org.smart.erp.system.dto.RoleMenuAssignDTO;
import org.smart.erp.system.dto.RolePermissionAssignDTO;
import org.smart.erp.system.dto.RoleUpdateDTO;
import org.smart.erp.system.entity.Menu;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.entity.RoleMenu;
import org.smart.erp.system.entity.RolePermission;
import org.smart.erp.system.mapper.RoleInfoMapper;
import org.smart.erp.system.mapper.RoleMenuMapper;
import org.smart.erp.system.mapper.RolePermissionMapper;
import org.smart.erp.system.service.RoleInfoService;
import org.smart.erp.system.vo.RoleInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleInfoServiceImpl extends ServiceImpl<RoleInfoMapper, RoleInfo> implements RoleInfoService {

    private final RoleInfoMapper roleInfoMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMenuMapper roleMenuMapper;

    public RoleInfoServiceImpl(RoleInfoMapper roleInfoMapper,
                               RolePermissionMapper rolePermissionMapper, RoleMenuMapper roleMenuMapper) {
        this.roleInfoMapper = roleInfoMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /**
     * 将 RoleInfo 转为 RoleInfoVO（不含权限列表），供列表/新增/更新复用。
     */
    private RoleInfoVO toVO(RoleInfo roleInfo) {
        RoleInfoVO vo = new RoleInfoVO();
        vo.setId(roleInfo.getId());
        vo.setName(roleInfo.getName());
        vo.setCode(roleInfo.getCode());
        vo.setSort(roleInfo.getSort());
        vo.setStatus(roleInfo.getStatus());
        vo.setPermissionIds(List.of());
        return vo;
    }

    @Override
    public Page<RoleInfoVO> listRole(RoleGetDTO dto) {
        LambdaQueryWrapper<RoleInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(dto.getName() != null, RoleInfo::getName, dto.getName());
        queryWrapper.like(dto.getCode() != null, RoleInfo::getCode, dto.getCode());
        queryWrapper.eq(dto.getStatus() != null, RoleInfo::getStatus, dto.getStatus());
        queryWrapper.orderByDesc(RoleInfo::getSort);

        Page<RoleInfo> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        return PageConvertUtils.convert(page, this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateDTO dto) {
        // 先按 id 查出已存在记录，避免静默更新 0 行
        RoleInfo exist = this.getById(dto.getId());
        if (exist == null) {
            throw new BusinessException(404, "角色不存在");
        }

        RoleInfo roleInfo = new RoleInfo();
        if (dto.getName() != null) roleInfo.setName(dto.getName());
        if (dto.getCode() != null) roleInfo.setCode(dto.getCode());
        if (dto.getStatus() != null) roleInfo.setStatus(dto.getStatus());

        LambdaQueryWrapper<RoleInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleInfo::getId, dto.getId());
        this.update(roleInfo, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleUpdateDTO dto) {
        RoleInfo roleInfo = new RoleInfo();
        LambdaQueryWrapper<RoleInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleInfo::getCode, dto.getCode());
        RoleInfo exist = roleInfoMapper.selectOne(queryWrapper);
        if (exist != null) {
            throw new BusinessException(400, "角色编码已存在");
        }
        if (dto.getName() != null) roleInfo.setName(dto.getName());
        if (dto.getCode() != null) roleInfo.setCode(dto.getCode());
        if (dto.getStatus() != null) roleInfo.setStatus(dto.getStatus());
        this.save(roleInfo);
    }

    /**
     * 角色详情。
     * <p>
     * 职责边界：本方法属于"角色模块"，只返回角色自身信息与该角色关联的权限 id 列表。
     * 权限的 name/code/type 等详情由权限模块负责，不在角色模块内拼装，
     * 以避免角色服务越界依赖权限表结构。前端如需权限详情，可拿 permissionIds 去调权限接口。
     */
    @Override
    public RoleInfoVO getRoleDetail(Long id) {
        RoleInfo roleInfo = this.getById(id);
        if (roleInfo == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 只查"该角色绑了哪些权限 id"，不查权限表详情
        LambdaQueryWrapper<RolePermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RolePermission::getRoleId, id);
        List<Long> permissionIds = rolePermissionMapper.selectList(queryWrapper).stream()
                .map(RolePermission::getPermissionId)
                .toList();

        RoleInfoVO vo = new RoleInfoVO();
        vo.setId(roleInfo.getId());
        vo.setName(roleInfo.getName());
        vo.setCode(roleInfo.getCode());
        vo.setSort(roleInfo.getSort());
        vo.setStatus(roleInfo.getStatus());
        vo.setPermissionIds(permissionIds);
        return vo;
    }

    /**
     * 给角色分配权限。
     * <p>
     * 说明：角色拥有哪些权限由"前端选择"决定，本方法不负责查询可选权限，
     * 只接收前端传来的角色 id 与权限 id 列表，以"全量覆盖"方式写入 sys_role_permission 对照表：
     * 先删除该角色已有的全部权限关联，再批量插入本次传入的权限 id。
     * 这样既保证最终状态与前端选择一致，又避免增量更新带来的脏数据。
     *
     * @param dto roleId 角色 id；permissionIds 要绑定的权限 id 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(RolePermissionAssignDTO dto) {
        // 1. 校验角色是否存在
        if (this.getById(dto.getRoleId()) == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 2. 删除该角色原有关联（全量覆盖的前提：先清后写）
        LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RolePermission::getRoleId, dto.getRoleId());
        rolePermissionMapper.delete(deleteWrapper);

        // 3. 批量写入本次前端选择的权限关联
        List<Long> permissionIds = dto.getPermissionIds();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> relations = permissionIds.stream().map(permissionId -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(dto.getRoleId());
                rp.setPermissionId(permissionId);
                return rp;
            }).toList();
            // 逐条 insert（BaseMapper 无批量 insert，数据量小可接受）
            relations.forEach(rolePermissionMapper::insert);
        }
    }

    @Override
    public void removeRole(Long id) {
        RoleInfo roleInfo = this.getById(id);
        if (roleInfo == null) {
            throw new BusinessException(404, "角色不存在");
        }
        List<RolePermission> rolePermission =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().
                                eq(RolePermission::getRoleId, id)
                );
        if (rolePermission != null) {
            throw new BusinessException(400, "角色已被使用，无法删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(RoleMenuAssignDTO dto) {
        if (this.getById(dto.getRoleId()) == null) {
            throw new BusinessException(404, "角色不存在");
        }

        LambdaQueryWrapper<RoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RoleMenu::getRoleId, dto.getRoleId());
        roleMenuMapper.delete(deleteWrapper);

        List<Long> menuIds = dto.getMenuIds();
        if (menuIds != null && !menuIds.isEmpty()) {
            List<RoleMenu> relations = menuIds.stream().map(menuId -> {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(dto.getRoleId());
                rm.setMenuId(menuId);
                return rm;
            }).toList();
            relations.forEach(roleMenuMapper::insert);
        }
    }


}
