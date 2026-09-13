package org.smart.erp.inventory.dto;

import lombok.Data;
import org.smart.erp.inventory.enums.FinishWarehousingStatus;

import java.time.LocalDateTime;

@Data
public class FinishWarehousingPageDto {

    private Integer pageNum;

    private Integer pageSize;

    private String warehousingNo;

    private Long productionOrderId;

    private Long productionReportId;

    private Long materialId;

    private Long warehouseId;

    private Long warehousingUserId;

    private FinishWarehousingStatus status;

    private LocalDateTime warehousingTime;
}
