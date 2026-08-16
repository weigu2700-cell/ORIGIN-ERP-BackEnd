package org.smart.erp.system.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.Status;

@Data
public class PermissionGetDTO {

    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    private String name;

    private String code;

    private PermissionType type;

    private Status status;

    private Long parentId;

}
