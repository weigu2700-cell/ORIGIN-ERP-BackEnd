package org.smart.erp.master.dto.WorkshopDTO;

import lombok.Data;

@Data
public class WorkshopListDTO {

    private Integer page;

    private Integer pageSize;

    private String name;

    private String code;

    private String factoryId;

}
