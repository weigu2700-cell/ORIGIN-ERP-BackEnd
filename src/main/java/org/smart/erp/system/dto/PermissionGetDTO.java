package org.smart.erp.system.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PermissionGetDTO {

    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    private String name;

    private String code;

    private Integer type;

    private Integer status;

    private Long parentId;

}
