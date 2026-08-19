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

    private String status;

    private String unit;

    private BigDecimal safetyStock;

    private String remark;

}
