package org.smart.erp.sales.vo;

import lombok.Data;
import org.smart.erp.sales.enums.SalesOrderStatus;

import java.math.BigDecimal;
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

    private BigDecimal totalAmount;

    private String remark;

    private SalesOrderStatus status;

    private List<SalesOrderItemVo> items;
}
