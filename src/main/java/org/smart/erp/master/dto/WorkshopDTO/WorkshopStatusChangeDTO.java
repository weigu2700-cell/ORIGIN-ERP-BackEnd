package org.smart.erp.master.dto.WorkshopDTO;

import lombok.Data;
import org.smart.erp.master.enums.WorkshopStatus;

@Data
public class WorkshopStatusChangeDTO {

    private WorkshopStatus status;
}
