package org.smart.erp.production.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BOMExplosionVo {

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private Integer level;

    /** 下级组成物料 */
    private List<BOMExplosionVo> children = new ArrayList<>();
}
