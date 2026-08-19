package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseType;

@Data
public class WarehouseVO {

    private Long id;

    private String name;

    private String code;

    private WarehouseType type;

    private String address;

    private String remark;

    private Long factoryId;

    private String factoryName;
}
