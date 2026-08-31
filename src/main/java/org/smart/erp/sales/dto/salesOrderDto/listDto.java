package org.smart.erp.sales.dto.salesOrderDto;

import lombok.Data;
import lombok.Getter;
import org.smart.erp.sales.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class listDto {

    private Integer pageNum;

    private Integer pageSize;

    private Long customerId;

    private String orderNo;

    private LocalDateTime orderDate;

    private SalesOrderStatus status;
}
