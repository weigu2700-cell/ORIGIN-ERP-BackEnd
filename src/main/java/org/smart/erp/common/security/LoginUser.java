package org.smart.erp.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
}
