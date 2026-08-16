package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.Status;

import java.util.List;

@Data
public class PermissionTreeVO {

    private Long id;

    private String name;

    private String code;

    private PermissionType type;

    private Long parentId;

    private String parentName;

    private Integer sort;

    private Status status;

    private String remark;

    /** 子权限列表，组装成树时填充；平铺列表场景下为 null */
    private List<PermissionTreeVO> children;
}
