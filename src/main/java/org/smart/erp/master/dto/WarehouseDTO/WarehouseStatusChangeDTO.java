package org.smart.erp.master.dto.WarehouseDto;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseStatus;

@Data
public class WarehouseStatusChangeDto {

    private WarehouseStatus status;
}
