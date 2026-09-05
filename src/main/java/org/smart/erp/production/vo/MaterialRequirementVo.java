package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialRequirementVo {

    private Long materialId;

    private String materialCode;

    private String materialName;

    // 毛需求量
    private BigDecimal grossQuantity;

    // 可用量
    private BigDecimal stockUsedAvailableQuantity;

    // 缺口量
    private BigDecimal shortageQuantity;

}
