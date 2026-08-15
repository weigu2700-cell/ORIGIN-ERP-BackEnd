package org.smart.erp.system.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    public LoginVO(String token) {
        this.token = token;
    }
}
