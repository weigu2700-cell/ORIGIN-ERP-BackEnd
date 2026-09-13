package org.smart.erp.master.dto.WorkshopDto;

import lombok.Data;
import org.smart.erp.master.enums.WorkshopStatus;

@Data
public class WorkshopStatusChangeDto {

    private WorkshopStatus status;
}
