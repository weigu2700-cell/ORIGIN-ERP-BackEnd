package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class PermissionVO {

    private Long id;

    private String name;

    private String code;

    private Integer type;

    private Long parentId;

    private String parentName;

    private Integer sort;

    private Integer status;

    private String remark;

    /** 子权限列表，组装成树时填充；平铺列表场景下为 null */
    private List<PermissionVO> children;
}
