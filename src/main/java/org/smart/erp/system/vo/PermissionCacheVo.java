package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.Status;

@Data
public class PermissionCacheVo {

    private Long id;

    private String name;

    private String code;

    private PermissionType type;

    private Long parentId;

    private int sort;

    private Status status;

    private String remark;
}
