package org.smart.erp.ai.toolResult;

import java.math.BigDecimal;

public record WarehouseStockResult(
        String warehouseName,
        BigDecimal onHand,
        BigDecimal reserved,
        BigDecimal available
) {
}