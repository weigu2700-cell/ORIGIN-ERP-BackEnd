package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;

@Data
public class pageProductionDemandDto {

    private Integer pageNum;

    private Integer pageSize;

    private ProductionSourceType sourceType;

    private String demandNo;

    private Long materialId;

    private ProductionStatus status;
}
