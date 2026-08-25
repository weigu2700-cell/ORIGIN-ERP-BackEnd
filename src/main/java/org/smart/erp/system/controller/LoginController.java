package org.smart.erp.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.system.dto.LoginDTO;
import org.smart.erp.system.service.LoginService;
import org.smart.erp.system.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/login")
@Tag(name = "登录认证", description = "用户登录获取 Token")
public class LoginController {

    private final LoginService loginService;


    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @Operation(summary = "用户登录")
    @PostMapping
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success(loginService.login(dto));
    }

}
