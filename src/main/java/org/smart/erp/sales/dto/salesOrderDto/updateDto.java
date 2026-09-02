package org.smart.erp.sales.dto.salesOrderDto;

import lombok.Data;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.dto.salesOrderItemDto.updateItemDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.enums.SalesOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class updateDto {


    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private String remark;

    private SalesOrderStatus status;

    private List<updateItemDto> items;

}
