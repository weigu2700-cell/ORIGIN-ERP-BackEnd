package org.smart.erp.master.dto.WarehouseDto;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.enums.WarehouseType;

@Data
public class WarehousePageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private String code;

    private String factoryId;

    private WarehouseType type;

    private WarehouseStatus status;
}
