package org.smart.erp.ai.result;

import java.math.BigDecimal;
import java.util.List;

public record MaterialStockToolResult(
        String materialCode,
        String materialName,
        BigDecimal totalOnHand,
        BigDecimal totalReserved,
        BigDecimal totalAvailable,
        List<WarehouseStockResult> warehouses
) {
}
