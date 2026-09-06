package org.smart.erp.master.dto.SupplierDTO;

import lombok.Data;

@Data
public class SupplierListDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String code;

    private String name;

    private String shortName;

    private String contactName;

    private String phone;

    private String email;
}
