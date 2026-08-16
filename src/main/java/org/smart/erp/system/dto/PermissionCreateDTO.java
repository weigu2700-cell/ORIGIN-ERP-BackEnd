package org.smart.erp.system.dto;

import lombok.Data;

@Data
public class PermissionCreateDTO {

    private String name;

    private String code;

    private Integer type;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private String remark;
}
