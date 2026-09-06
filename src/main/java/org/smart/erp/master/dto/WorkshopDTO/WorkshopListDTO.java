package org.smart.erp.master.dto.WorkshopDTO;

import lombok.Data;

@Data
public class WorkshopListDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private String code;

    private String factoryId;

}
