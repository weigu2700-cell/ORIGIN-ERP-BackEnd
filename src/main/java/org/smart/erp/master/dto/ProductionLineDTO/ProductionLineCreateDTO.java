package org.smart.erp.master.dto.ProductionLineDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionLineCreateDTO {

    private String name;

    private Long workshopId;

    private BigDecimal capacityPerDay;

    private String remark;
}
