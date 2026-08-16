package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
import org.smart.erp.system.dto.UserRoleAssignDTO;
import org.smart.erp.system.dto.UserUpdateDTO;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.LoginVO;
import org.smart.erp.system.vo.UserCreateVO;
import org.smart.erp.system.vo.UserGetVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/system/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/create")
    public Result<Void> create(@RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<UserGetVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<Page<UserGetVO>> getUserList(UserGetDTO dto) {
        return Result.success(userService.getUserList(dto));
    }

    /**
     * 给用户分配角色。
     * 路径 {id} 为用户 id，请求体传要绑定的角色 id 列表。
     * 只负责把前端选择的角色写入 sys_user_role 对照表（查可选角色由其它接口完成）。
     */
    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @RequestBody UserRoleAssignDTO dto) {
        dto.setUserId(id);
        userService.assignRoles(dto);
        return Result.success(null);
    }

}
