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

    /** 1 启用，0 停用 */
    private Integer status;
}
