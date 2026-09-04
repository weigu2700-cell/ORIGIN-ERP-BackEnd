package org.smart.erp.master.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialVO {

    private Long id;

    private String code;

    private String name;

    private String spec;

    private String type;

    /** 1 启用，0 停用 */
    private Integer status;

    private String unit;

    private BigDecimal safetyStock;

    private String remark;

}
