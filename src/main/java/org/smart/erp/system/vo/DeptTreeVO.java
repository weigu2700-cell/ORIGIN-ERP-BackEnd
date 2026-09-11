package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptTreeVo {

    private String id;

    private String name;

    private String code;

    private String parentId;

    private List<DeptTreeVo> children;
}
