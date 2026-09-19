package org.smart.erp.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.dto.toolResult.MaterialStockToolResult;
import org.smart.erp.ai.dto.toolResult.WarehouseStockResult;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.service.MaterialService;
import org.smart.erp.master.service.WarehouseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryTool {

    private final MaterialStockService materialStockService;
    private final WarehouseService warehouseService;
    private final MaterialService materialService;

    @Tool(name = "get_material_stock", description = "根据物料编码获取物料在各仓库的库存明细与合计（在库、预留、可用量）")
    public MaterialStockToolResult getMaterialStock(String materialCode) {
        Material material = materialService.getOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getCode, materialCode)
        );
        if (material == null) {
            return new MaterialStockToolResult(
                    materialCode,
                    null,
                    null,
                    null,
                    null, List.of()
            );
        }

        List<MaterialStock> stocks = materialStockService.list(new LambdaQueryWrapper<MaterialStock>()
                .eq(MaterialStock::getMaterialId, material.getId()));

        BigDecimal totalOnHand = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        List<WarehouseStockResult> warehouses = new ArrayList<>();

        for (MaterialStock stock : stocks) {
            BigDecimal onHand = stock.getOnHand() != null ? stock.getOnHand() : BigDecimal.ZERO;
            BigDecimal reserved = stock.getReserved() != null ? stock.getReserved() : BigDecimal.ZERO;
            BigDecimal available = onHand.subtract(reserved);

            totalOnHand = totalOnHand.add(onHand);
            totalReserved = totalReserved.add(reserved);
            totalAvailable = totalAvailable.add(available);

            Warehouse warehouse = warehouseService.getById(stock.getWarehouseId());
            String warehouseName = warehouse != null ? warehouse.getName() : null;
            warehouses.add(new WarehouseStockResult(warehouseName, onHand, reserved, available));
        }

        return new MaterialStockToolResult(
                material.getCode(),
                material.getName(),
                stocks.isEmpty() ? null : totalOnHand,
                stocks.isEmpty() ? null : totalReserved,
                stocks.isEmpty() ? null : totalAvailable,
                warehouses
        );
    }
}
