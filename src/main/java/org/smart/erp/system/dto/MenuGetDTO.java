package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.Status;
import org.springframework.stereotype.Component;

@Data
public class MenuGetDTO {

    private int page;

    private int pageSize;

    private Long title;

    private Long id;

    private String name;

    private String parentId;

    private int visible;

    private Status status;
}
