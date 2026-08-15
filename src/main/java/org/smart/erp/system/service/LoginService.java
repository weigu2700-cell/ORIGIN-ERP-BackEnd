package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.vo.LoginVO;

public interface LoginService extends IService<User> {

    LoginVO login(LoginDTO dto);
}
