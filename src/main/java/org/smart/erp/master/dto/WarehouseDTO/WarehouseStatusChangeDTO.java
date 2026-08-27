package org.smart.erp.master.dto.WarehouseDTO;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseStatus;

@Data
public class WarehouseStatusChangeDTO {

    private WarehouseStatus status;
}
