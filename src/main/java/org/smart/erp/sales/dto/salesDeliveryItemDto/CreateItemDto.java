package org.smart.erp.sales.dto.salesDeliveryItemDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateItemDto {

    private Long deliveryId;

    private Integer lineNo;

    private Long salesOrderItemId;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal quantity;

}
