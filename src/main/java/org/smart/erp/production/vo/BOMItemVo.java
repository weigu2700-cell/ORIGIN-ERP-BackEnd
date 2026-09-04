package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BOMItemVo {

    private Long id;

    private Long bomId;

    private Integer lineNo;

    private Long componentMaterialId;

    private String componentMaterialName;

    private String componentMaterialCode;

    private BigDecimal quantity;

    private BigDecimal lossRate;

    private String remark;
}
