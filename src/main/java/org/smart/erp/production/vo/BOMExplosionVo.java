package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BOMExplosionVo {

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private Integer level;
}

