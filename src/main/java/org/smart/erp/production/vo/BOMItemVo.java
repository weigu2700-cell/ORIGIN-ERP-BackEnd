package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BOMItemVo {

    private Long id;

    private Long bomId;

    private Integer lineNo;

    private Long componentMaterialId;

    private String componentMaterialCode;

    private String componentMaterialName;

    private BigDecimal quantity;

    private BigDecimal lossRate;

    private String remark;

}
