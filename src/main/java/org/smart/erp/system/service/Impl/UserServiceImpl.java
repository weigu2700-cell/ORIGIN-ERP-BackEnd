package org.smart.erp.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.DeptMapper;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.LoginVO;
import org.smart.erp.system.vo.UserCreateVO;
import org.smart.erp.system.vo.UserGetVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DeptMapper deptMapper;

    public UserServiceImpl(UserMapper userMapper , PasswordEncoder passwordEncoder, DeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.deptMapper = deptMapper;
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
        newUser.setRealName(dto.getRealName() != null ? dto.getRealName() : dto.getUsername());
        newUser.setStatus(UserStatus.NORMAL);
        newUser.setDeptId(dto.getDeptId());

        userMapper.insert(newUser);
        UserCreateVO vo = new UserCreateVO();
        BeanUtils.copyProperties(newUser, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserGetVO getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserGetVO vo = new UserGetVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        return vo;
    }

}
