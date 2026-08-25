package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
import org.smart.erp.system.dto.UserRoleAssignDTO;
import org.smart.erp.system.dto.UserUpdateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.mapper.*;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.UserGetVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CurrentUser currentUser;

    public UserServiceImpl(
            UserMapper userMapper ,
            PasswordEncoder passwordEncoder,
            DeptMapper deptMapper,
            RoleInfoMapper roleInfoMapper,
            UserRoleMapper userRoleMapper,
            CurrentUser currentUser)
    {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.deptMapper = deptMapper;
        this.roleInfoMapper = roleInfoMapper;
        this.userRoleMapper = userRoleMapper;
        this.currentUser = currentUser;
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
    public void createUser(UserCreateDTO dto) {
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
    public UserGetVO getCurrentUserInfo() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return getUserDetail(userId);
    }


    @Override
    public void updateUser(Long id, UserUpdateDTO dto) {
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

        userMapper.updateById(user);
    }

    /**
     * 给用户分配角色。
     * <p>
     * 说明：用户拥有哪些角色由"前端选择"决定，本方法不负责查询可选角色，
     * 只接收前端传来的用户 id 与角色 id 列表，以"全量覆盖"方式写入 sys_user_role 对照表：
     * 先删除该用户已有的全部角色关联，再批量插入本次传入的角色 id。
     * 既保证最终状态与前端选择一致，又避免增量更新带来的脏数据。
     *
     * @param id userId 用户 id；roleIds 要绑定的角色 id 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 逻辑删除（@TableLogic -> deleted=1）
        removeById(id);
        // 清理用户-角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(UserRoleAssignDTO dto) {
        // 1. 校验用户是否存在
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 2. 删除该用户原有关联（全量覆盖的前提：先清后写）
        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, dto.getUserId());
        userRoleMapper.delete(deleteWrapper);

        // 3. 批量写入本次前端选择的角色关联
        List<Long> roleIds = dto.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> relations = roleIds.stream().map(roleId -> {
                UserRole ur = new UserRole();
                ur.setUserId(dto.getUserId());
                ur.setRoleId(roleId);
                return ur;
            }).toList();
            // 逐条 insert（BaseMapper 无批量 insert，数据量小可接受）
            relations.forEach(userRoleMapper::insert);
        }
    }

    @Override
    public Page<UserGetVO> listUser(UserGetDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (dto.getUsername() != null) queryWrapper.like(User::getUsername, dto.getUsername());
        if (dto.getPhone() != null) queryWrapper.like(User::getPhone, dto.getPhone());
        if (dto.getDeptId() != null) queryWrapper.eq(User::getDeptId, dto.getDeptId());
        if (dto.getStatus() != null) queryWrapper.eq(User::getStatus, dto.getStatus());
        queryWrapper.orderByDesc(User::getId);

        Page<User> page = new Page<>(dto.getPage(), dto.getPageSize());
        userMapper.selectPage(page, queryWrapper);
        return PageConvertUtils.convert(page, this::toUserGetVO);
    }

    private UserGetVO toUserGetVO(User user) {
        UserGetVO vo = new UserGetVO();
        BeanUtils.copyProperties(user, vo);
        vo.setDeptName(resolveDeptName(user.getDeptId()));
        vo.setRoles(buildUserRoles(user.getId()));
        return vo;
    }

}
