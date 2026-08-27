package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
import org.smart.erp.system.dto.UserRoleAssignDTO;
import org.smart.erp.system.dto.UserUpdateDTO;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.UserGetVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/user")
@Tag(name = "用户管理", description = "用户的增删改查、列表与角色分配")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @Operation(summary = "新增用户")
    @PostMapping("/create")
    public Result<Void> create(@RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id:\\d+}")
    public Result<UserGetVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @Operation(summary = "用户详情（兼容 /detail/{id} 路径）")
    @GetMapping("/detail/{id:\\d+}")
    public Result<UserGetVO> getUserDetailByPath(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @Operation(summary = "删除用户（兼容 /delete/{id} 路径）")
    @RequestMapping(value = "/delete/{id:\\d+}", method = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST})
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id:\\d+}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "更新用户（兼容 /update 路径）")
    @RequestMapping(value = "/update/{id:\\d+}", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<Void> updateUserCompat(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    public Result<Page<UserGetVO>> listUser(UserGetDTO dto) {
        return Result.success(userService.listUser(dto));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<UserGetVO> getCurrentUser() {
        return Result.success(userService.getCurrentUserInfo());
    }

    /**
     * 给用户分配角色。
     * 路径 {id} 为用户 id，请求体传要绑定的角色 id 列表。
     * 只负责把前端选择的角色写入 sys_user_role 对照表（查可选角色由其它接口完成）。
     */
    @Operation(summary = "为用户分配角色")
    @PostMapping("/{id:\\d+}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @RequestBody UserRoleAssignDTO dto) {
        dto.setUserId(id);
        userService.assignRoles(dto);
        return Result.success(null);
    }

}
