package org.smart.erp.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {

    private String username;

    private String password;

    private String realName;

    private Long deptId;

    private List<Long> roleIds;

}
