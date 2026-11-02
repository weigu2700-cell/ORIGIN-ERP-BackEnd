package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.dto.LoginDto;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.LoginService;
import org.smart.erp.system.vo.LoginVo;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.registry.TenantResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl extends ServiceImpl<UserMapper, User> implements LoginService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TenantResolver tenants;

    public LoginServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
                            TenantResolver tenants) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.tenants = tenants;
    }

    @Override
    public LoginVo login(LoginDto dto) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null)
            throw new BusinessException(400, "请输入账号和密码");
        var tenant = tenants.login(dto.getTenantCode());
        // 必须先确定企业，再查询用户和进入后续事务。
        try (var scope = TenantScope.open(tenant.id())) {
            var queryWrapper = new LambdaQueryWrapper<User>();
            queryWrapper.eq(User::getUsername, dto.getUsername());

            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                throw new BusinessException(404, "用户不存在");
            }
            if (user.getStatus() != UserStatus.NORMAL) {
                throw new BusinessException(403, "账号不可用");
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BusinessException(400, "密码错误");
            }

            return new LoginVo(jwtUtil.generateToken(user.getId(), tenant.id()));
        } catch (DataAccessException error) {
            throw new TenantException(503, "企业数据库暂时不可用");
        }
    }
}
