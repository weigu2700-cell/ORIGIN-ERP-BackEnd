package org.smart.erp.master.dto.WorkshopDto;

import lombok.Data;

@Data
public class WorkshopPageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private String code;

    private String factoryId;

}
