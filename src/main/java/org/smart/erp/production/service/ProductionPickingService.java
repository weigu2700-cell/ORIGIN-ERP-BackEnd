package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.production.dto.ProductionPickingAddDto;
import org.smart.erp.production.dto.ProductionPickingPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionPicking;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionPickingVo;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProductionPickingService extends IService<ProductionPicking> {
    void addProductionPicking(ProductionPickingAddDto dto);

    /** 按条件分页查询领料单（返回展示 Vo） */
    @PreAuthorize("hasAnyAuthority('production:picking:list')")
    Page<ProductionPickingVo> pageProductionPicking(ProductionPickingPageDto dto);

    /** 获取领料单详情（返回与分页列表一致的展示 Vo）。 */
    @PreAuthorize("hasAnyAuthority('production:picking:get')")
    ProductionPickingVo getProductionPicking(Long id);

    /**
     * 由生产订单按 BOM 生成领料单：
     * - 在库物料：选择有可用库存的仓库预留后，状态置“已审批/可领料”
     * - 缺料物料：与采购需求一一对应，状态置“草稿/待采购入库”，待采购入库后通知领料
     *
     * @param order               生产订单
     * @param requirements        BOM 物料净需求（含毛需求/缺口量）
     * @param demandIdByMaterial  物料ID -> 采购需求ID（仅缺料物料，用于一一对应）
     */
    void generatePickingFromOrder(
            ProductionOrder order,
            List<MaterialRequirementVo> requirements,
            Map<Long, Long> demandIdByMaterial);

    /** 领料确认：库存出库、状态置“已领料”，若同订单全部领完则下达生产 */
    @PreAuthorize("hasAnyAuthority('production:picking:confirm')")
    void confirmPicking(Long id);

    /** 采购入库上架后，通知对应缺料领料单可领料（指定入库仓库并预留） */
    void notifyPickingForInStock(Long materialId, Long warehouseId);

    @PreAuthorize("hasAnyAuthority('production:picking:approve')")
    void approvePicking(Long id);
}
