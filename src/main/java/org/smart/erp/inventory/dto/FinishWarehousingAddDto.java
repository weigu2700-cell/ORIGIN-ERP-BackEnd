package org.smart.erp.inventory.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinishWarehousingAddDto {

    private Long productionOrderId;

    private Long productionReportId;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal warehousingQuantity;

    private Long warehousingUserId;

    private LocalDateTime warehousingTime;

    private String remark;
}
