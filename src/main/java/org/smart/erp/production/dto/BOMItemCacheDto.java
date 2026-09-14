package org.smart.erp.production.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BOMItemCacheDto {

    private Long id;

    private Long componentMaterialId;

    private BigDecimal quantity;

    private BigDecimal lossRate;

    private Integer lineNo;
}
