package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.RoleEnum;

@Data
public class RoleGetDTO {

    private int page = 1;

    private int pageSize = 10;

    private long id;

    private String name;

    private String code;

    private int sort;

    private RoleEnum status;
}
