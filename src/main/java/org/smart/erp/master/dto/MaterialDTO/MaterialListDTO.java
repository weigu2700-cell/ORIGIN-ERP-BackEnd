package org.smart.erp.master.dto.MaterialDTO;

import lombok.Data;

@Data
public class MaterialListDTO {

    private Integer page;

    private Integer pageSize;

    private String name;

    private String code;

    private String spec;

    private String type;

    private String status;
}
