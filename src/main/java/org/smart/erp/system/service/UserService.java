package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.LoginDto;
import org.smart.erp.system.dto.UserAddDto;
import org.smart.erp.system.dto.UserDetailDto;
import org.smart.erp.system.dto.UserRoleAssignDto;
import org.smart.erp.system.dto.UserStatusUpdateDto;
import org.smart.erp.system.dto.UserUpdateDto;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.vo.LoginVo;
import org.smart.erp.system.vo.UserCreateVo;
import org.smart.erp.system.vo.UserDetailVo;

import java.util.List;

public interface UserService extends IService<User> {
    User detailUserById(Long id);

    /** 新增用户，仅落库不返回数据 */
    void addUser(UserAddDto dto);

    UserDetailVo detailUser(Long id);

    /** 更新用户，仅落库不返回数据 */
    void updateUser(Long id, UserUpdateDto dto);

    /** 修改用户状态 */
    void updateUserStatus(Long id, UserStatusUpdateDto dto);

    Page<UserDetailVo> pageUser(UserDetailDto dto);

    /** 获取当前登录用户信息（含部门、角色） */
    UserDetailVo getCurrentUserInfo();

    void assignRoles(UserRoleAssignDto dto);

    /** 删除用户（逻辑删除，同时清理角色关联） */
    void removeUser(Long id);
}
