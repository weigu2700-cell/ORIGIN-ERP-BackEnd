package org.smart.erp.master.dto.SupplierDto;

import lombok.Data;

@Data
public class SupplierAddDto {

    private String name;

    private String shortName;

    private String contactName;

    private String address;

    private String phone;

    private String email;

    private String remark;
}
