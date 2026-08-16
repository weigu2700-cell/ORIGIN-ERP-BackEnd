package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;

    private String name;

    private String title;

    private String path;

    private String component;

    private String icon;

    private Long parentId;

    private List<MenuTreeVO> children;
}
