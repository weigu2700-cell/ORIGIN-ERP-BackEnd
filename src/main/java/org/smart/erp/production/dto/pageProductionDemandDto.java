package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;

@Data
public class pageProductionDemandDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private ProductionSourceType sourceType;

    private String demandNo;

    private Long materialId;

    private ProductionStatus status;
}
