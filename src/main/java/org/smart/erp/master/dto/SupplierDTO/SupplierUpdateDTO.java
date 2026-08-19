package org.smart.erp.master.dto.SupplierDTO;

import lombok.Data;

@Data
public class SupplierUpdateDTO {

    private String name;

    private String shortName;

    private String contactName;

    private String address;

    private String phone;

    private String email;

    private String remark;
}
