package org.smart.erp.master.dto.FactoryDTO;

import lombok.Data;

@Data
public class FactoryListDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private String code;

    private String shortName;

}
