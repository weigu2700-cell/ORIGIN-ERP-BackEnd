package org.smart.erp.sales.dto.salesOrderDto;

import lombok.Data;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class createDto {

    private Long customerId;

    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private String remark;

    private List<createItemDto> items;
}
