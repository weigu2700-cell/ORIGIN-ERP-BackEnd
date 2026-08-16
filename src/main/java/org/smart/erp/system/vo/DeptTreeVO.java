package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptTreeVO {

    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private List<DeptTreeVO> children;
}
