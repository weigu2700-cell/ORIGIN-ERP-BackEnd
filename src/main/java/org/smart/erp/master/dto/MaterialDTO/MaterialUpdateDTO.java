package org.smart.erp.master.dto.MaterialDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialUpdateDto {

    private String name;

    private String spec;

    private String unit;

    private BigDecimal safetyStock;

    private String remark;

}
