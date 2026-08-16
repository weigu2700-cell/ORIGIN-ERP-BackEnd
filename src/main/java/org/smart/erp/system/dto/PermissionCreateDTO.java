package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.Status;

@Data
public class PermissionCreateDTO {

    private String name;

    private String code;

    private PermissionType type;

    private Long parentId;

    private Integer sort;

    private Status status;

    private String remark;
}
