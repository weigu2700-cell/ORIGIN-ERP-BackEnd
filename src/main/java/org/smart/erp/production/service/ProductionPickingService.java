package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.ProductionPickingAddDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionPicking;
import org.smart.erp.production.vo.MaterialRequirementVo;

import java.util.List;
import java.util.Map;

public interface ProductionPickingService extends IService<ProductionPicking> {
    void addProductionPicking(ProductionPickingAddDto dto);

    /**
     * 由生产订单按 BOM 生成领料单：
     * - 在库物料：选择有可用库存的仓库预留后，状态置“已审批/可领料”
     * - 缺料物料：与采购需求一一对应，状态置“草稿/待采购入库”，待采购入库后通知领料
     *
     * @param order               生产订单
     * @param requirements        BOM 物料净需求（含毛需求/缺口量）
     * @param demandIdByMaterial  物料ID -> 采购需求ID（仅缺料物料，用于一一对应）
     */
    void generatePickingFromOrder(ProductionOrder order, List<MaterialRequirementVo> requirements,
                                  Map<Long, Long> demandIdByMaterial);

    /** 领料确认：库存出库、状态置“已领料”，若同订单全部领完则下达生产 */
    void confirmPicking(Long id);

    /** 采购入库上架后，通知对应缺料领料单可领料（指定入库仓库并预留） */
    void notifyPickingForInStock(Long materialId, Long warehouseId);
}
