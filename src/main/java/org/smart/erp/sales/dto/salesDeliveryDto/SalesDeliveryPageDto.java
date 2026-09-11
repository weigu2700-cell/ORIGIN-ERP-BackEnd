package org.smart.erp.sales.dto.salesDeliveryDto;

import lombok.Data;
import org.smart.erp.sales.enums.SalesDeliveryStatus;

@Data
public class SalesDeliveryPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String deliveryNo;

    private Long salesOrderId;

    private Long customerId;

    private SalesDeliveryStatus status;
}
