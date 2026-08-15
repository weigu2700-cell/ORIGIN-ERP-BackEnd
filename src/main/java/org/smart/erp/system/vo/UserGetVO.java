package org.smart.erp.system.vo;

import lombok.Data;

@Data
public class UserGetVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String deptName;
}
