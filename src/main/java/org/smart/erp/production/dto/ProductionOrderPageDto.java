package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.ProductionOrderStatus;

import java.time.LocalDateTime;

@Data
public class ProductionOrderPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String productionOrderNo;

    private String productionDemandNo;

    private String materialId;

    private ProductionOrderStatus status;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

}
