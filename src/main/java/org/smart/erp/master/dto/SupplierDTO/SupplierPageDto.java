package org.smart.erp.master.dto.SupplierDto;

import lombok.Data;

@Data
public class SupplierPageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String code;

    private String name;

    private String shortName;

    private String contactName;

    private String phone;

    private String email;
}
