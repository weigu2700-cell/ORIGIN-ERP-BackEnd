package org.smart.erp.ai.tool;

import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.dto.toolResult.MaterialStockToolResult;
import org.smart.erp.ai.dto.toolResult.WarehouseStockResult;
import org.smart.erp.inventory.dto.MaterialStockPageDto;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.vo.MaterialStockVo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryTool {

    private final MaterialStockService materialStockService;

    @Tool(name = "get_material_stock", description = "根据物料编码获取物料在各仓库的库存明细与合计（在库、预留、可用量）")
    public MaterialStockToolResult getMaterialStock(String materialCode) {
        MaterialStockPageDto dto = new MaterialStockPageDto();
        dto.setMaterialCode(materialCode);
        dto.setPageNum(1);
        dto.setPageSize(Integer.MAX_VALUE);

        List<MaterialStockVo> stocks = materialStockService.pageMaterialStock(dto).getRecords();
        if (stocks.isEmpty()) {
            return new MaterialStockToolResult(materialCode, null, null, null, null, List.of());
        }

        BigDecimal totalOnHand = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        List<WarehouseStockResult> warehouses = new ArrayList<>();

        for (MaterialStockVo stock : stocks) {
            BigDecimal onHand = stock.getOnHand() != null ? stock.getOnHand() : BigDecimal.ZERO;
            BigDecimal reserved = stock.getReserved() != null ? stock.getReserved() : BigDecimal.ZERO;
            BigDecimal available = onHand.subtract(reserved);

            totalOnHand = totalOnHand.add(onHand);
            totalReserved = totalReserved.add(reserved);
            totalAvailable = totalAvailable.add(available);

            warehouses.add(new WarehouseStockResult(stock.getWarehouseName(), onHand, reserved, available));
        }

        return new MaterialStockToolResult(
                materialCode,
                stocks.getFirst().getMaterialName(),
                totalOnHand,
                totalReserved,
                totalAvailable,
                warehouses
        );
    }
}
