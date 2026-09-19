package org.smart.erp.ai.dto.toolResult;

import java.math.BigDecimal;

public record WarehouseStockResult(
        String warehouseName,
        BigDecimal onHand,
        BigDecimal reserved,
        BigDecimal available
) {
}