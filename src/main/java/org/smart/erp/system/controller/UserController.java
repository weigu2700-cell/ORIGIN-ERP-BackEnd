package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.LoginDto;
import org.smart.erp.system.dto.UserAddDto;
import org.smart.erp.system.dto.UserDetailDto;
import org.smart.erp.system.dto.UserRoleAssignDto;
import org.smart.erp.system.dto.UserStatusUpdateDto;
import org.smart.erp.system.dto.UserUpdateDto;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.vo.UserDetailVo;
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
    public Result<Void> add(@RequestBody UserAddDto dto) {
        userService.addUser(dto);
        return Result.success();
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id:\\d+}")
    public Result<UserDetailVo> detailUser(@PathVariable Long id) {
        return Result.success(userService.detailUser(id));
    }

    @Operation(summary = "用户详情（兼容 /detail/{id} 路径）")
    @GetMapping("/detail/{id:\\d+}")
    public Result<UserDetailVo> getUserDetailByPath(@PathVariable Long id) {
        return Result.success(userService.detailUser(id));
    }

    @Operation(summary = "删除用户（兼容 /delete/{id} 路径）")
    @RequestMapping(value = "/delete/{id:\\d+}", method = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST})
    public Result<Void> removeUser(@PathVariable Long id) {
        userService.removeUser(id);
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id:\\d+}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateDto dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "更新用户（兼容 /update 路径）")
    @RequestMapping(value = "/update/{id:\\d+}", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<Void> updateUserCompat(@PathVariable Long id, @RequestBody UserUpdateDto dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id:\\d+}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusUpdateDto dto) {
        userService.updateUserStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    public Result<Page<UserDetailVo>> pageUser(UserDetailDto dto) {
        return Result.success(userService.pageUser(dto));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<UserDetailVo> getCurrentUser() {
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
                                    @RequestBody UserRoleAssignDto dto) {
        dto.setUserId(id);
        userService.assignRoles(dto);
        return Result.success(null);
    }

}
