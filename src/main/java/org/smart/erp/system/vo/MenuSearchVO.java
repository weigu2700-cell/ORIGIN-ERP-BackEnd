package org.smart.erp.system.vo;

import lombok.Data;

@Data
public class MenuSearchVO {

    private Long id;

    private String title;

    private String path;

    private String icon;

    private String parentTitle;
}
