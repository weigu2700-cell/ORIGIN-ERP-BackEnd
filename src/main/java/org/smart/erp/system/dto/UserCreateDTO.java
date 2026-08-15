package org.smart.erp.system.dto;

import lombok.Data;

@Data
public class UserCreateDTO {

    private String username;

    private String password;

    private String realName;

    private Long deptId;

}
