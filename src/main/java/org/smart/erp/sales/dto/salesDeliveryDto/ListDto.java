package org.smart.erp.sales.dto.salesDeliveryDto;

import lombok.Data;
import org.smart.erp.sales.enums.SalesDeliveryStatus;

@Data
public class ListDto {

    private Integer pageNum;

    private Integer pageSize;

    private String deliveryNo;

    private Long salesOrderId;

    private Long customerId;

    private SalesDeliveryStatus status;
}
