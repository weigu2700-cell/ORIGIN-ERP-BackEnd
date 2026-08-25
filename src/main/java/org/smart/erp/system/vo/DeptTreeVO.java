package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptTreeVO {

    private String id;

    private String name;

    private String code;

    private String parentId;

    private List<DeptTreeVO> children;
}
