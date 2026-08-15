package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.UserStatus;

@Data
public class UserGetDTO {

    private int page = 1;

    private int pageSize = 10;

    private String username;

    private Long deptId;

    private UserStatus status;

    private String phone;


}
