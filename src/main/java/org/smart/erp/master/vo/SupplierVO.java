package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.entity.Supplier;

@Data
public class SupplierVO {

    private Long id;

    private String code;

    private String name;

    private String shortName;

    private String contactName;

    private String address;

    private String phone;

    private String email;

    private String remark;

}
