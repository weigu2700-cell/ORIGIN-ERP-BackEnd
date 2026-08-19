package org.smart.erp.master.dto.MaterialDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialUpdateDTO {

    private String name;

    private String spec;

    private String unit;

    private BigDecimal safetyStock;

    private String remark;

}
