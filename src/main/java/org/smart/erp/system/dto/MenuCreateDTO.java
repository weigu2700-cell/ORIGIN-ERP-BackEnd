package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.Status;

@Data
public class MenuCreateDTO {

    private String name;

    private String title;

    private String path;

    private String component;

    private String icon;

    private Long parentId;

    private Integer visible;

    private Status status;
}
