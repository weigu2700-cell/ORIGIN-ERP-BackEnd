package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.Status;
import org.springframework.stereotype.Component;

@Data
public class MenuListVO {

    private Long id;

    private String name;

    private String title;

    private String path;

    private Long parentId;

    private String parentName;

    private String component;

    private String icon;

    private Integer visible;

    private Status status;

}
