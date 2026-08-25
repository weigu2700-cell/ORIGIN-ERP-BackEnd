package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
import org.smart.erp.system.dto.UserRoleAssignDTO;
import org.smart.erp.system.dto.UserUpdateDTO;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.vo.LoginVO;
import org.smart.erp.system.vo.UserCreateVO;
import org.smart.erp.system.vo.UserGetVO;

import java.util.List;

public interface UserService extends IService<User> {
    User getUserById(Long id);

    /** 新增用户，仅落库不返回数据 */
    void createUser(UserCreateDTO dto);

    UserGetVO getUserDetail(Long id);

    /** 更新用户，仅落库不返回数据 */
    void updateUser(Long id, UserUpdateDTO dto);

    Page<UserGetVO> listUser(UserGetDTO dto);

    /** 获取当前登录用户信息（含部门、角色） */
    UserGetVO getCurrentUserInfo();

    void assignRoles(UserRoleAssignDTO dto);

    /** 删除用户（逻辑删除，同时清理角色关联） */
    void deleteUser(Long id);
}
