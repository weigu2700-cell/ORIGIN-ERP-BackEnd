package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.Status;
import org.springframework.stereotype.Component;

@Data
public class MenuGetDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String title;

    private Long id;

    private String name;

    private Long parentId;

    private Integer visible;

    private Status status;
}
