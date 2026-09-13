package org.smart.erp.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinishWarehousingVo {

    private Long id;

    private Long productionOrderId;

    private Long productionReportId;

    private Long materialId;

    private Long warehouseId;

    private String warehousingNo;

    private String productionOrderNo;

    private String productionReportNo;

    private String materialName;

    private String materialCode;

    private String warehouseName;

    private BigDecimal warehousingQuantity;

    private String warehousingUserName;

    private LocalDateTime warehousingTime;

    private String remark;
}
