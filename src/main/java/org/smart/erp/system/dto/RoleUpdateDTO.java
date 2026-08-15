package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.RoleEnum;

import java.util.List;

@Data
public class RoleUpdateDTO {

    private long id;

    private String name;

    private String code;

    private RoleEnum status;

    private List<String> permissionCode;
}
