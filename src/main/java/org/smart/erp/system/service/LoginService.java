package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.LoginDto;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.vo.LoginVo;

public interface LoginService extends IService<User> {

    LoginVo login(LoginDto dto);
}
