package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionOrderVo {

    private Long id;

    private Long productionDemandId;

    private String productionDemandNo;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal plannedQuantity;

    private BigDecimal completedQuantity;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private String remark;

    private String productionOrderNo;

    private Integer status;
}
