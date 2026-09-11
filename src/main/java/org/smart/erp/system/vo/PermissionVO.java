package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.Status;

@Data
public class PermissionVo {

    private Long id;

    private String name;

    private String code;

    private PermissionType type;

    private Long parentId;

    private String parentName;

    private Integer sort;

    private Status status;

    private String remark;
}
