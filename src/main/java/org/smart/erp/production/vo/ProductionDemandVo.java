package org.smart.erp.production.vo;

import lombok.Data;

import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;

import java.math.BigDecimal;

@Data
public class ProductionDemandVo {

    private Long id;

    private String demandNo;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private ProductionSourceType sourceType;

    private String sourceNo;

    private ProductionStatus status;

}
