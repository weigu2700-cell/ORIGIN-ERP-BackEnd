package org.smart.erp.system.controller;

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
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;


    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success(loginService.login(dto));
    }

}
