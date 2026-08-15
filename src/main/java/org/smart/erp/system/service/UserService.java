package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.vo.LoginVO;
import org.smart.erp.system.vo.UserCreateVO;
import org.smart.erp.system.vo.UserGetVO;

public interface UserService extends IService<User> {
    User getUserById(Long id);

    UserCreateVO createUser(UserCreateDTO dto);

    UserGetVO getUserDetail(Long id);
}
