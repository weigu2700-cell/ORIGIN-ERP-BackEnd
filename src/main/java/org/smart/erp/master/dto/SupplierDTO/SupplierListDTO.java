package org.smart.erp.master.dto.SupplierDTO;

import lombok.Data;

@Data
public class SupplierListDTO {

    private Integer page;

    private Integer pageSize;

    private String code;

    private String name;

    private String shortName;

    private String contactName;

    private String phone;

    private String email;
}
