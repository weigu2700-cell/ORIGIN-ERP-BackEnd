package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.LoginService;
import org.smart.erp.system.vo.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl extends ServiceImpl<UserMapper, User> implements LoginService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        queryWrapper.eq(User::getUsername, dto.getUsername());

        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String userPassword = user.getPassword();
        String inputPassword = dto.getPassword();
        if (!passwordEncoder.matches(inputPassword, userPassword)) {
            throw new BusinessException(400, "密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        return new LoginVO(token);

    }

}
