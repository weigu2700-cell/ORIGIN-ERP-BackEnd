package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.UserStatus;

import java.util.List;

@Data
public class UserUpdateDTO {

    private long id;

    private String username;

    private String password;

    private List<Long> roleIds;

    private Long deptId;

    private UserStatus status;

    private String phone;
}
