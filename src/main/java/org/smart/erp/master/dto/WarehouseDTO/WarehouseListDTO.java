package org.smart.erp.master.dto.WarehouseDTO;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.enums.WarehouseType;

@Data
public class WarehouseListDTO {

    private Integer page;

    private Integer pageSize;

    private String name;

    private String code;

    private String factoryId;

    private WarehouseType type;

    private WarehouseStatus status;
}
