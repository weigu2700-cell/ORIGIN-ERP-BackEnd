package org.smart.erp.master.dto.ProductionLineDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionLineUpdateDto {

    private String name;

    private Long workshopId;

    private BigDecimal capacityPerDay;

    private String remark;

}
