package org.smart.erp.system.vo;

import lombok.Data;

@Data
public class LoginVo {

    private String token;

    public LoginVo(String token) {
        this.token = token;
    }
}
