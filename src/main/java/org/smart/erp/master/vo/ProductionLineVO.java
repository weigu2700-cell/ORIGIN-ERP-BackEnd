package org.smart.erp.master.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionLineVO {

    private Long id;

    private String name;

    private Long workshopId;

    private String workshopName;

    private BigDecimal capacityPerDay;

    private String remark;

    /** 1 启用，0 停用 */
    private Integer status;

    private String createTime;

}
