package org.smart.erp.sales.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesOrderVo {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private String remark;

    private Integer status;

    private List<SalesOrderItemVo> items;
}
