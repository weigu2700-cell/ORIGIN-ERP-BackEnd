package org.smart.erp.master.dto.WarehouseDTO;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseType;

@Data
public class WarehouseCreateDTO {

    private String name;

    private WarehouseType type;

    private Long factoryId;

    private String address;

    private String remark;


}
