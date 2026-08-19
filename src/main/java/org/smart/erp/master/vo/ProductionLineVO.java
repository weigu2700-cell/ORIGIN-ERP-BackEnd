package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.enums.ProductionLineStatus;

import java.math.BigDecimal;

@Data
public class ProductionLineVO {

    private Long id;

    private String name;

    private Long workshopId;

    private String workshopName;

    private BigDecimal capacityPerDay;

    private String remark;

    private ProductionLineStatus status;

    private String createTime;

}
