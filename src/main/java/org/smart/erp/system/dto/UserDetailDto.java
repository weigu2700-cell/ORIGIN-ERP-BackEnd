package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.UserStatus;

@Data
public class UserDetailDto {

    private int page = 1;

    private int pageSize = 10;

    private String username;

    private String realName;

    private Long deptId;

    private UserStatus status;

    private String phone;


}
