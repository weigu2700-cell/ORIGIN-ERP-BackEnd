package org.smart.erp.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
import org.smart.erp.system.dto.UserUpdateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.mapper.*;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.UserCreateVO;
import org.smart.erp.system.vo.UserGetVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.smart.erp.system.entity.RoleInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DeptMapper deptMapper;
    private final RoleInfoMapper roleInfoMapper;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(
            UserMapper userMapper ,
            PasswordEncoder passwordEncoder,
            DeptMapper deptMapper,
            RoleInfoMapper roleInfoMapper,
            UserRoleMapper userRoleMapper)
    {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.deptMapper = deptMapper;
        this.roleInfoMapper = roleInfoMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 根据用户 id 查询其拥有的角色列表（一对多）。
     * 通过关联表 UserRole 查出角色 id，再批量查询角色信息，避免 N+1。
     *
     * @param userId 用户 id
     * @return 角色 Map 列表，每个元素含 roleName、roleCode
     */
    private List<Map<String, Object>> buildUserRoles(Long userId) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper);
        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .toList();
        List<RoleInfo> roleInfos = roleInfoMapper.selectList(
                new LambdaQueryWrapper<RoleInfo>().in(RoleInfo::getId, roleIds));
        Map<Long, RoleInfo> roleMapById = roleInfos.stream()
                .collect(java.util.stream.Collectors.toMap(RoleInfo::getId, r -> r));

        return userRoles.stream().map(userRole -> {
            Map<String, Object> roleMap = new HashMap<>();
            RoleInfo roleInfo = roleMapById.get(userRole.getRoleId());
            if (roleInfo != null) {
                roleMap.put("roleName", roleInfo.getName());
                roleMap.put("roleCode", roleInfo.getCode());
            }
            return roleMap;
        }).toList();
    }

    /**
     * 根据用户 deptId 解析部门名称，无部门或部门不存在时返回 null。
     */
    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Dept dept = deptMapper.selectById(deptId);
        return dept != null ? dept.getName() : null;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public UserCreateVO createUser(UserCreateDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername());

        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setStatus(UserStatus.NORMAL);
        newUser.setDeptId(dto.getDeptId());

        // 先落库拿到自增/雪花 id，再写用户-角色关联
        userMapper.insert(newUser);

        List<Long> roleIds = dto.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(newUser.getId());
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .toList();
            userRoles.forEach(userRoleMapper::insert);
        }

        UserCreateVO vo = new UserCreateVO();
        BeanUtils.copyProperties(newUser, vo);
        return vo;
    }

    @Override
    public UserGetVO getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserGetVO vo = new UserGetVO();
        BeanUtils.copyProperties(user, vo);
        vo.setDeptName(resolveDeptName(user.getDeptId()));
        vo.setRoles(buildUserRoles(id));
        return vo;
    }


    @Override
    public UserGetVO updateUser(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        // 密码更新时用 BCrypt 加密存储
        if (dto.getPassword() != null) user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());

        if (dto.getRoleIds() != null) {
            LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserRole::getUserId, id);
            userRoleMapper.delete(queryWrapper);

            List<UserRole> userRoles = dto.getRoleIds().stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(id);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .toList();
            userRoles.forEach(userRoleMapper::insert);
        }

        userMapper.updateById(user);

        UserGetVO vo = new UserGetVO();
        BeanUtils.copyProperties(user, vo);
        vo.setDeptName(resolveDeptName(user.getDeptId()));
        return vo;
    }

    @Override
    public Page<UserGetVO> getUserList(UserGetDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (dto.getUsername() != null) queryWrapper.like(User::getUsername, dto.getUsername());
        if (dto.getPhone() != null) queryWrapper.like(User::getPhone, dto.getPhone());
        if (dto.getDeptId() != null) queryWrapper.eq(User::getDeptId, dto.getDeptId());
        if (dto.getStatus() != null) queryWrapper.eq(User::getStatus, dto.getStatus());
        queryWrapper.orderByDesc(User::getId);

        Page<User> page = new Page<>(dto.getPage(), dto.getPageSize());
        userMapper.selectPage(page, queryWrapper);

        List<UserGetVO> vos = page.getRecords().stream().map(user -> {
            UserGetVO vo = new UserGetVO();
            BeanUtils.copyProperties(user, vo);
            vo.setDeptName(resolveDeptName(user.getDeptId()));
            vo.setRoles(buildUserRoles(user.getId()));
            return vo;
        }).toList();

        Page<UserGetVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

}
