package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.dto.UserCreateDTO;
import org.smart.erp.system.dto.UserGetDTO;
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
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/create")
    public Result<UserCreateVO> create(@RequestBody UserCreateDTO dto) {
        return Result.success(userService.createUser(dto));
    }

    @GetMapping("/{id}")
    public Result<UserGetVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @PutMapping("/{id}")
    public Result<UserGetVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        return Result.success(userService.updateUser(id, dto));
    }

    @GetMapping("/list")
    public Result<Page<UserGetVO>> getUserList(UserGetDTO dto) {
        return Result.success(userService.getUserList(dto));
    }

}
