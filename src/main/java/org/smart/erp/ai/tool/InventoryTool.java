package org.smart.erp.ai.tool;

import lombok.RequiredArgsConstructor;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.vo.MaterialStockVo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryTool {

    private final MaterialStockService materialStockService;

    @Tool(name = "get_material_stock", description = "根据物料编码获取物料库存信息")
    public MaterialStockVo getMaterialStock(String materialCode) {
        return materialStockService.getMaterialStockByCode(materialCode);
    }
}
