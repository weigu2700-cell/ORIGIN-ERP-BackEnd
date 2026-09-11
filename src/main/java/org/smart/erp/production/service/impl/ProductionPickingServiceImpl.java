package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.production.dto.ProductionPickingAddDto;
import org.smart.erp.production.dto.ProductionPickingPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionPicking;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionPickingStatus;
import org.smart.erp.production.mapper.ProductionOrderMapper;
import org.smart.erp.production.mapper.ProductionPickingMapper;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.service.ProductionPickingService;
import org.smart.erp.production.vo.BOMExplosionVo;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionPickingVo;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductionPickingServiceImpl
    extends ServiceImpl<ProductionPickingMapper, ProductionPicking>
    implements ProductionPickingService
{

    private final BusinessNoGenerator businessNoGenerator;
    private final ProductionOrderMapper productionOrderMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final PurchaseDemandMapper purchaseDemandMapper;
    private final BOMService bomService;
    private final MaterialStockService materialStockService;

    public ProductionPickingServiceImpl(
            BusinessNoGenerator businessNoGenerator,
            ProductionOrderMapper productionOrderMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper,
            PurchaseDemandMapper purchaseDemandMapper,
            BOMService bomService,
            MaterialStockService materialStockService)
    {
        this.businessNoGenerator = businessNoGenerator;
        this.productionOrderMapper = productionOrderMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.purchaseDemandMapper = purchaseDemandMapper;
        this.bomService = bomService;
        this.materialStockService = materialStockService;
    }

    /**
     * 构造一条领料单的基础信息（生产订单、物料、计划数量、领料单号、初始实际数量）。
     * 状态、仓库、关联采购需求等由调用方按业务补充。
     *
     * @param order       生产订单（取订单ID）
     * @param materialId  领料物料ID
     * @param plannedQty  计划领料数量（在库部分传库存可用量，缺料部分传缺口量）
     * @return 已填充基础字段的领料单实体（未持久化）
     */
    private ProductionPicking buildPicking(ProductionOrder order, Long materialId, BigDecimal plannedQty) {
        ProductionPicking picking = new ProductionPicking();
        picking.setProductionOrderId(order.getId());
        picking.setMaterialId(materialId);
        picking.setPlannedQuantity(plannedQty);
        picking.setActualQuantity(BigDecimal.ZERO);
        picking.setPickingNo(businessNoGenerator.generateNo("erp:sequence:production-picking:", "PICK"));
        return picking;
    }

    /**
     * 选择可用库存足以覆盖需求量的仓库（多仓库时取首个满足者）。
     * 可用量 = 在库(onHand) - 预留(reserved)，下限为 0。
     *
     * @param materialId 物料ID
     * @param qty        需求量
     * @return 满足需求的仓库ID；若没有任何单个仓库可用量足以覆盖需求量则返回 null
     */
    private Long findWarehouseCovering(Long materialId, BigDecimal qty) {
        List<MaterialStock> stocks = materialStockService.list(
                new LambdaQueryWrapper<MaterialStock>().eq(MaterialStock::getMaterialId, materialId));
        for (MaterialStock s : stocks) {
            if (availableOf(s).compareTo(qty) >= 0) {
                return s.getWarehouseId();
            }
        }
        return null;
    }

    /**
     * 计算指定仓库中某物料的可用量（在库 - 预留，下限为 0）。
     * 若仓库无该物料库存记录则返回 0。
     *
     * @param materialId  物料ID
     * @param warehouseId 仓库ID
     * @return 可用量
     */
    private BigDecimal availableAt(Long materialId, Long warehouseId) {
        MaterialStock stock = materialStockService.getOne(
                new LambdaQueryWrapper<MaterialStock>()
                        .eq(MaterialStock::getMaterialId, materialId)
                        .eq(MaterialStock::getWarehouseId, warehouseId));
        return stock == null ? BigDecimal.ZERO : availableOf(stock);
    }

    /**
     * 计算单条库存记录的可用量：在库(onHand) - 预留(reserved)，下限为 0。
     *
     * @param s 库存记录
     * @return 可用量
     */
    private BigDecimal availableOf(MaterialStock s) {
        BigDecimal onHand = s.getOnHand() == null ? BigDecimal.ZERO : s.getOnHand();
        BigDecimal reserved = s.getReserved() == null ? BigDecimal.ZERO : s.getReserved();
        return onHand.subtract(reserved).max(BigDecimal.ZERO);
    }

    /**
     * 递归收集 BOM 展开树中的所有组成物料ID（含各级子件）。
     *
     * @param vo  当前展开节点
     * @param acc 用于累积物料ID的集合（递归共享）
     */
    private void collectComponentMaterialIds(BOMExplosionVo vo, Set<Long> acc) {
        if (vo.getMaterialId() != null) {
            acc.add(vo.getMaterialId());
        }
        if (vo.getChildren() != null) {
            for (BOMExplosionVo child : vo.getChildren()) {
                collectComponentMaterialIds(child, acc);
            }
        }
    }

    @Override
    public void addProductionPicking(ProductionPickingAddDto dto) {
        // 1. 生产订单必须存在
        ProductionOrder order = productionOrderMapper.selectById(dto.getProductionOrderId());
        if (order == null) {
            throw new BusinessException(404, "生产订单不存在");
        }

        // 2. 领料物料必须存在
        if (dto.getMaterialId() == null || materialMapper.selectById(dto.getMaterialId()) == null) {
            throw new BusinessException(400, "领料物料不存在");
        }

        // 3. 领料仓库必须存在
        if (dto.getWarehouseId() == null || warehouseMapper.selectById(dto.getWarehouseId()) == null) {
            throw new BusinessException(400, "领料仓库不存在");
        }

        // 4. 物料必须是该生产订单产品 BOM 的组成物料
        Set<Long> componentIds = new HashSet<>();
        List<BOMExplosionVo> explosion = bomService.getBOMExplosion(
                order.getMaterialId(), order.getPlannedQuantity());
        for (BOMExplosionVo vo : explosion) {
            collectComponentMaterialIds(vo, componentIds);
        }
        if (!componentIds.contains(dto.getMaterialId())) {
            throw new BusinessException(400, "物料不属于该生产订单BOM组成");
        }

        // 5. 计划领料数量必须大于 0
        if (dto.getPlannedQuantity() == null
                || dto.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "计划领料数量必须大于0");
        }

        ProductionPicking productionPicking = new ProductionPicking();
        BeanUtils.copyProperties(dto, productionPicking);
        productionPicking.setPickingNo(
                businessNoGenerator.generateNo("erp:sequence:production-picking:", "PICK"));
        productionPicking.setStatus(ProductionPickingStatus.DRAFT);
        if (productionPicking.getActualQuantity() == null) {
            productionPicking.setActualQuantity(BigDecimal.ZERO);
        }
        save(productionPicking);

        // 说明：草稿领料单仅登记计划，实际库存出库（materialStockService.outboundStock）
        // 应在领料单确认/领料（状态流转至 PICKED）时执行，而非新增草稿阶段。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePickingFromOrder(ProductionOrder order,
                                         List<MaterialRequirementVo> requirements,
                                         Map<Long, Long> demandIdByMaterial) {
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        for (MaterialRequirementVo req : requirements) {
            Long materialId = req.getMaterialId();
            BigDecimal gross = req.getGrossQuantity();
            if (materialId == null || gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal stockUsed = req.getStockUsedAvailableQuantity() == null
                    ? BigDecimal.ZERO : req.getStockUsedAvailableQuantity();
            BigDecimal shortage = req.getShortageQuantity() == null
                    ? BigDecimal.ZERO : req.getShortageQuantity();

            // 1) 在库可覆盖部分：直接可领料，从有库存的仓库预留
            if (stockUsed.compareTo(BigDecimal.ZERO) > 0) {
                ProductionPicking stockPicking = buildPicking(order, materialId, stockUsed);
                Long warehouseId = findWarehouseCovering(materialId, stockUsed);
                if (warehouseId != null) {
                    materialStockService.reserveStock(materialId, warehouseId, stockUsed,
                            "PRODUCTION_PICKING", stockPicking.getPickingNo(), "生产备料预留");
                    stockPicking.setWarehouseId(warehouseId);
                    stockPicking.setStatus(ProductionPickingStatus.APPROVED);
                } else {
                    // 无单个仓库可覆盖可用量的边界情况：仍登记，待人工指定仓库
                    stockPicking.setStatus(ProductionPickingStatus.DRAFT);
                }
                save(stockPicking);
            }

            // 2) 缺料部分：与采购需求一一对应（数量 = 缺口量），待采购入库后通知领料
            if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                Long demandId = demandIdByMaterial.get(materialId);
                ProductionPicking shortPicking = buildPicking(order, materialId, shortage);
                if (demandId != null) {
                    shortPicking.setPurchaseDemandId(demandId);
                }
                shortPicking.setStatus(ProductionPickingStatus.DRAFT);
                save(shortPicking);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPicking(Long id) {
        ProductionPicking picking = getById(id);
        if (picking == null) {
            throw new BusinessException(404, "领料单不存在");
        }
        if (picking.getStatus() != ProductionPickingStatus.APPROVED) {
            throw new BusinessException(400, "领料单未处于可领料状态");
        }
        if (picking.getWarehouseId() == null) {
            throw new BusinessException(400, "领料仓库未指定，无法领料");
        }

        BigDecimal qty = (picking.getActualQuantity() != null
                && picking.getActualQuantity().compareTo(BigDecimal.ZERO) > 0)
                ? picking.getActualQuantity() : picking.getPlannedQuantity();

        // 库存出库（扣减在库与预留）
        materialStockService.outboundStock(picking.getMaterialId(), picking.getWarehouseId(), qty,
                "PRODUCTION_PICKING", picking.getPickingNo(), "生产领料出库");

        picking.setActualQuantity(qty);
        picking.setPickingTime(LocalDateTime.now());
        picking.setStatus(ProductionPickingStatus.PICKED);
        updateById(picking);

        // 同订单全部领料完成则自动下达生产
        List<ProductionPicking> all = this.list(new LambdaQueryWrapper<ProductionPicking>()
                .eq(ProductionPicking::getProductionOrderId, picking.getProductionOrderId()));
        boolean allPicked = all.stream()
                .allMatch(p -> p.getStatus() == ProductionPickingStatus.PICKED);
        if (allPicked) {
            ProductionOrder order = productionOrderMapper.selectById(picking.getProductionOrderId());
            if (order != null && order.getStatus() == ProductionOrderStatus.RELEASED) {
                order.setStatus(ProductionOrderStatus.IN_PROGRESS);
                order.setActualStartTime(LocalDateTime.now());
                productionOrderMapper.updateById(order);
            }
        }
    }

    @Override
    public Page<ProductionPickingVo> pageProductionPicking(ProductionPickingPageDto dto) {
        LambdaQueryWrapper<ProductionPicking> queryWrapper =
                new LambdaQueryWrapper<ProductionPicking>()
                    .eq(Objects.nonNull(dto.getProductionOrderId()),
                            ProductionPicking::getProductionOrderId, dto.getProductionOrderId())

                    .eq(Objects.nonNull(dto.getPurchaseDemandId()),
                            ProductionPicking::getPurchaseDemandId, dto.getPurchaseDemandId())

                    .eq(Objects.nonNull(dto.getMaterialId()),
                            ProductionPicking::getMaterialId, dto.getMaterialId())

                    .eq(Objects.nonNull(dto.getWarehouseId()),
                            ProductionPicking::getWarehouseId, dto.getWarehouseId())

                    .eq(Objects.nonNull(dto.getStatus()),
                            ProductionPicking::getStatus, dto.getStatus())

                    .ge(Objects.nonNull(dto.getPickingTimeStart()),
                            ProductionPicking::getPickingTime, dto.getPickingTimeStart())

                    .le(Objects.nonNull(dto.getPickingTimeEnd()),
                            ProductionPicking::getPickingTime, dto.getPickingTimeEnd())

                    .orderByAsc(ProductionPicking::getCreateTime);

        Page<ProductionPicking> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);

        Set<Long> materialIds = page.getRecords().stream()
                .map(ProductionPicking::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> warehouseIds = page.getRecords().stream()
                .map(ProductionPicking::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> orderIds = page.getRecords().stream()
                .map(ProductionPicking::getProductionOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> demandIds = page.getRecords().stream()
                .map(ProductionPicking::getPurchaseDemandId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Collections.emptyMap()
                : materialMapper.selectByIds(materialIds).stream()
                    .collect(Collectors.toMap(Material::getId, m -> m));

        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseMapper.selectByIds(warehouseIds).stream()
                    .collect(Collectors.toMap(Warehouse::getId, w -> w));

        Map<Long, ProductionOrder> orderMap = orderIds.isEmpty()
                ? Collections.emptyMap()
                : productionOrderMapper.selectByIds(orderIds).stream()
                    .collect(Collectors.toMap(ProductionOrder::getId, o -> o));

        Map<Long, PurchaseDemand> demandMap = demandIds.isEmpty()
                ? Collections.emptyMap()
                : purchaseDemandMapper.selectByIds(demandIds).stream()
                    .collect(Collectors.toMap(PurchaseDemand::getId, d -> d));

        List<ProductionPickingVo> voList = page.getRecords().stream().map(picking -> {
            ProductionPickingVo vo = new ProductionPickingVo();
            BeanUtils.copyProperties(picking, vo);

            Material material = materialMap.get(picking.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }
            Warehouse warehouse = warehouseMap.get(picking.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            ProductionOrder order = orderMap.get(picking.getProductionOrderId());
            if (order != null) {
                vo.setProductionOrderNo(order.getProductionOrderNo());
            }
            PurchaseDemand demand = demandMap.get(picking.getPurchaseDemandId());
            if (demand != null) {
                vo.setPurchaseDemandNo(demand.getPurchaseDemandNo());
            }
            return vo;
        }).toList();

        Page<ProductionPickingVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyPickingForInStock(Long materialId, Long warehouseId) {
        if (materialId == null || warehouseId == null) {
            return;
        }
        // 仅通知“缺料、待采购入库”的领料单（与采购需求一一对应）
        List<ProductionPicking> waiting = this.list(new LambdaQueryWrapper<ProductionPicking>()
                .eq(ProductionPicking::getMaterialId, materialId)
                .eq(ProductionPicking::getStatus, ProductionPickingStatus.DRAFT)
                .isNotNull(ProductionPicking::getPurchaseDemandId));
        if (waiting.isEmpty()) {
            return;
        }
        BigDecimal available = availableAt(materialId, warehouseId);
        for (ProductionPicking p : waiting) {
            BigDecimal qty = p.getPlannedQuantity(); // = 缺口量
            BigDecimal reserveQty = qty.min(available.max(BigDecimal.ZERO));
            p.setWarehouseId(warehouseId);
            if (reserveQty.compareTo(BigDecimal.ZERO) > 0) {
                materialStockService.reserveStock(p.getMaterialId(), warehouseId, reserveQty,
                        "PRODUCTION_PICKING", p.getPickingNo(), "采购入库后生产备料预留");
            }
            // 入库量足以覆盖缺口才可领料，否则继续等待后续入库
            p.setStatus(reserveQty.compareTo(qty) >= 0
                    ? ProductionPickingStatus.APPROVED : ProductionPickingStatus.DRAFT);
            updateById(p);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePicking(Long id) {
        if (id == null) {
            throw new BusinessException(400, "领料单ID不能为空");
        }
        ProductionPicking productionPicking = this.getById(id);
        if (productionPicking == null) {
            throw new BusinessException(400, "领料单不存在");
        }
        if (!ProductionPickingStatus.DRAFT.equals(productionPicking.getStatus())) {
            throw new BusinessException(400, "当前状态不为草稿，无法审核");
        }

        productionPicking.setStatus(ProductionPickingStatus.APPROVED);
        this.updateById(productionPicking);
    }
}
