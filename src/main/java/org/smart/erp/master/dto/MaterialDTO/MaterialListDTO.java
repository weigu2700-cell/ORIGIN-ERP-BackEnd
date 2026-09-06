package org.smart.erp.master.dto.MaterialDTO;

import lombok.Data;

@Data
public class MaterialListDTO {

    private int page = 1;

    private int pageSize = 10;

    private String name;

    private String code;

    private String spec;

    private String type;

    private String status;
}
