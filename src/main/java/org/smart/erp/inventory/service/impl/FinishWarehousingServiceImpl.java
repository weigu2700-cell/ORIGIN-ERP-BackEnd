package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.dto.FinishWarehousingAddDto;
import org.smart.erp.inventory.dto.FinishWarehousingPageDto;
import org.smart.erp.inventory.entity.FinishWarehousing;
import org.smart.erp.inventory.enums.FinishWarehousingStatus;
import org.smart.erp.inventory.mapper.FinishWarehousingMapper;
import org.smart.erp.inventory.service.FinishWarehousingService;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.vo.FinishWarehousingVo;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.service.MaterialService;
import org.smart.erp.master.service.WarehouseService;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionReport;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.event.ProductionReportFinishedEvent;
import org.smart.erp.production.service.ProductionReportService;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinishWarehousingServiceImpl
    extends ServiceImpl<FinishWarehousingMapper, FinishWarehousing>
    implements FinishWarehousingService
{

    private final BusinessNoGenerator businessNoGenerator;
    private final ProductionOrderService productionOrderService;
    private final ProductionReportService productionReportService;
    private final MaterialService materialService;
    private final WarehouseService warehouseService;
    private final UserService userService;
    private final MaterialStockService materialStockService;

    public FinishWarehousingServiceImpl(
            BusinessNoGenerator businessNoGenerator,
            ProductionOrderService productionOrderService,
            ProductionReportService productionReportService,
            MaterialService materialService,
            WarehouseService warehouseService,
            UserService userService,
            MaterialStockService materialStockService
    ) {
        this.businessNoGenerator = businessNoGenerator;
        this.productionOrderService = productionOrderService;
        this.productionReportService = productionReportService;
        this.materialService = materialService;
        this.warehouseService = warehouseService;
        this.userService = userService;
        this.materialStockService = materialStockService;
    }

    /**
     * 检查对象是否为空，为空则抛出业务异常
     * @param obj 对象
     * @param message 异常信息
     */
    private void checkNull(Object obj, String message) {
        Optional.ofNullable(obj)
                .orElseThrow(() -> new BusinessException(400, message));
    }

    /**
     * 修改入库单状态
     * @param id 入库单ID
     * @param requireCheckStatus 前置状态（执行转换前必须处于该状态）
     * @param status 目标状态
     * @return 是否更新成功
     */
    private FinishWarehousing changeFinishWarehousingStatus(
            Long id,
            FinishWarehousingStatus requireCheckStatus,
            FinishWarehousingStatus status
    ) {
        checkNull(id, "ID不能为空");
        checkNull(requireCheckStatus, "前置状态不能为空");
        checkNull(status, "目标状态不能为空");

        FinishWarehousing finishWarehousing = getById(id);
        checkNull(finishWarehousing, "入库单不存在");
        checkNull(finishWarehousing.getStatus(), "入库单状态不能为空");

        if (!requireCheckStatus.equals(finishWarehousing.getStatus())) {
            throw new BusinessException(400, "入库单状态不匹配，无法执行该操作");
        }

        finishWarehousing.setStatus(status);
        updateById(finishWarehousing);
        return finishWarehousing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFinishWarehousing(FinishWarehousingAddDto dto) {
        checkNull(dto.getProductionOrderId(), "生产单ID不能为空");
        checkNull(dto.getProductionReportId(), "生产报工ID不能为空");
        checkNull(dto.getMaterialId(), "物料ID不能为空");
        checkNull(dto.getWarehouseId(), "仓库ID不能为空");
        checkNull(dto.getWarehousingQuantity(), "入库数量不能为空");
        checkNull(dto.getWarehousingUserId(), "入库人ID不能为空");

        FinishWarehousing finishWarehousing = new FinishWarehousing();
        BeanUtils.copyProperties(dto, finishWarehousing);

        finishWarehousing.setWarehousingNo(
                businessNoGenerator.generateNo("erp:sequence:finish-warehousing:", "FW"));
        finishWarehousing.setStatus(FinishWarehousingStatus.DRAFT);
        finishWarehousing.setWarehousingTime(
                Optional.ofNullable(finishWarehousing.getWarehousingTime())
                        .orElse(LocalDateTime.now()));

        // P0: 成品入库数量不能超过对应报工的合格数量（含已入库累计）
        ProductionReport report = productionReportService.getById(dto.getProductionReportId());
        checkNull(report, "关联报工单不存在");
        if (report.getQualifiedQuantity() == null) {
            throw new BusinessException(400, "报工合格数量为空，无法入库");
        }
        BigDecimal alreadyWarehoused = getTotalWarehousingQuantityByReport(dto.getProductionReportId());
        if (alreadyWarehoused.add(dto.getWarehousingQuantity()).compareTo(report.getQualifiedQuantity()) > 0) {
            throw new BusinessException(400, "入库数量超过报工合格数量（合格量 "
                    + report.getQualifiedQuantity() + "，已入库 " + alreadyWarehoused + "）");
        }

        save(finishWarehousing);
    }

    /**
     * 监听报工完成事件，自动生成一张草稿态成品入库单（入库量取报工合格量）。
     * 事件在同事务内同步触发，若报工整体回滚则入库单一并回滚。
     */
    @EventListener
    public void handleProductionReportFinished(ProductionReportFinishedEvent event) {
        FinishWarehousingAddDto fwDto = new FinishWarehousingAddDto();
        fwDto.setProductionOrderId(event.getProductionOrderId());
        fwDto.setProductionReportId(event.getProductionReportId());
        fwDto.setMaterialId(event.getMaterialId());
        fwDto.setWarehouseId(event.getWarehouseId());
        fwDto.setWarehousingQuantity(event.getQualifiedQuantity());
        fwDto.setWarehousingUserId(event.getWarehousingUserId());
        addFinishWarehousing(fwDto);
    }

    /** 统计某报工单已入库（WAREHOUSED）的成品数量，用于校验不超报工合格量 */
    private BigDecimal getTotalWarehousingQuantityByReport(Long productionReportId) {
        if (productionReportId == null) {
            return BigDecimal.ZERO;
        }
        List<FinishWarehousing> list = this.list(
                new LambdaQueryWrapper<FinishWarehousing>()
                        .eq(FinishWarehousing::getProductionReportId, productionReportId)
                        .eq(FinishWarehousing::getStatus, FinishWarehousingStatus.WAREHOUSED));
        return list.stream()
                .map(FinishWarehousing::getWarehousingQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Page<FinishWarehousingVo> pageFinishWarehousing(FinishWarehousingPageDto dto) {
        LambdaQueryWrapper<FinishWarehousing> qw =
                new LambdaQueryWrapper<FinishWarehousing>()
                        .like(Objects.nonNull(dto.getWarehousingNo()),
                                FinishWarehousing::getWarehousingNo, dto.getWarehousingNo())
                        .eq(Objects.nonNull(dto.getProductionOrderId()),
                                FinishWarehousing::getProductionOrderId, dto.getProductionOrderId())
                        .eq(Objects.nonNull(dto.getProductionReportId()),
                                FinishWarehousing::getProductionReportId, dto.getProductionReportId())
                        .eq(Objects.nonNull(dto.getMaterialId()),
                                FinishWarehousing::getMaterialId, dto.getMaterialId())
                        .eq(Objects.nonNull(dto.getWarehouseId()),
                                FinishWarehousing::getWarehouseId, dto.getWarehouseId())
                        .eq(Objects.nonNull(dto.getWarehousingUserId()),
                                FinishWarehousing::getWarehousingUserId, dto.getWarehousingUserId())
                        .eq(Objects.nonNull(dto.getStatus()),
                                FinishWarehousing::getStatus, dto.getStatus())
                        .ge(Objects.nonNull(dto.getWarehousingTime()),
                                FinishWarehousing::getWarehousingTime, dto.getWarehousingTime());

        Page<FinishWarehousing> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), qw);

        Set<Long> productionOrderIds = page.getRecords().stream()
                .map(FinishWarehousing::getProductionOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> productionReportIds = page.getRecords().stream()
                .map(FinishWarehousing::getProductionReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> materialIds = page.getRecords().stream()
                .map(FinishWarehousing::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> warehouseIds = page.getRecords().stream()
                .map(FinishWarehousing::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> warehousingUserIds = page.getRecords().stream()
                .map(FinishWarehousing::getWarehousingUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductionOrder> productionOrderMap = productionOrderIds.isEmpty()
                ? Map.of()
                : productionOrderService.listByIds(productionOrderIds)
                        .stream()
                        .collect(Collectors.toMap(ProductionOrder::getId, Function.identity()));

        Map<Long, ProductionReport> productionReportMap = productionReportIds.isEmpty()
                ? Map.of()
                : productionReportService.listByIds(productionReportIds)
                        .stream()
                        .collect(Collectors.toMap(ProductionReport::getId, Function.identity()));

        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Map.of()
                : materialService.listByIds(materialIds)
                        .stream()
                        .collect(Collectors.toMap(Material::getId, Function.identity()));

        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty()
                ? Map.of()
                : warehouseService.listByIds(warehouseIds)
                        .stream()
                        .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        Map<Long, User> userMap = warehousingUserIds.isEmpty()
                ? Map.of()
                : userService.listByIds(warehousingUserIds)
                        .stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<FinishWarehousingVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        return voPage.setRecords(page.getRecords().stream()
                .map(finishWarehousing -> {
                    FinishWarehousingVo vo = new FinishWarehousingVo();
                    BeanUtils.copyProperties(finishWarehousing, vo);
                    Optional.ofNullable(productionOrderMap.get(finishWarehousing.getProductionOrderId()))
                            .ifPresent(order -> vo.setProductionOrderNo(order.getProductionOrderNo()));
                    Optional.ofNullable(productionReportMap.get(finishWarehousing.getProductionReportId()))
                            .ifPresent(report -> vo.setProductionReportNo(report.getProductionReportNo()));
                    Optional.ofNullable(materialMap.get(finishWarehousing.getMaterialId()))
                            .ifPresent(material -> {
                                vo.setMaterialName(material.getName());
                                vo.setMaterialCode(material.getCode());
                            });
                    Optional.ofNullable(warehouseMap.get(finishWarehousing.getWarehouseId()))
                            .ifPresent(warehouse -> vo.setWarehouseName(warehouse.getName()));
                    Optional.ofNullable(userMap.get(finishWarehousing.getWarehousingUserId()))
                            .ifPresent(user -> vo.setWarehousingUserName(user.getUsername()));
                    return vo;
                }).toList()
        );
    }

    @Override
    public FinishWarehousingVo getFinishWarehousing(Long id) {
        FinishWarehousing finishWarehousing = getById(id);
        checkNull(finishWarehousing, "入库单不存在");

        FinishWarehousingVo vo = new FinishWarehousingVo();
        BeanUtils.copyProperties(finishWarehousing, vo);

        Optional.ofNullable(finishWarehousing.getProductionOrderId())
                .map(productionOrderService::getById)
                .ifPresent(order -> vo.setProductionOrderNo(order.getProductionOrderNo()));

        Optional.ofNullable(finishWarehousing.getProductionReportId())
                .map(productionReportService::getById)
                .ifPresent(report -> vo.setProductionReportNo(report.getProductionReportNo()));

        Optional.ofNullable(finishWarehousing.getMaterialId())
                .map(materialService::getById)
                .ifPresent(material -> {
                    vo.setMaterialName(material.getName());
                    vo.setMaterialCode(material.getCode());
                });

        Optional.ofNullable(finishWarehousing.getWarehouseId())
                .map(warehouseService::getById)
                .ifPresent(warehouse -> vo.setWarehouseName(warehouse.getName()));

        Optional.ofNullable(finishWarehousing.getWarehousingUserId())
                .map(userService::getById)
                .ifPresent(user -> vo.setWarehousingUserName(user.getUsername()));

        return vo;
    }

    @Override
    public Boolean approveFinishWarehousing(Long id) {
        changeFinishWarehousingStatus(
                id,
                FinishWarehousingStatus.DRAFT,
                FinishWarehousingStatus.APPROVED
        );
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean warehouseFinishWarehousing(Long id) {
        FinishWarehousing finishWarehousing = changeFinishWarehousingStatus(
                id, FinishWarehousingStatus.APPROVED, FinishWarehousingStatus.WAREHOUSED);

        checkNull(finishWarehousing.getMaterialId(), "入库单未关联物料，无法入库");
        checkNull(finishWarehousing.getWarehouseId(), "入库单未指定仓库，无法入库");
        checkNull(finishWarehousing.getWarehousingQuantity(), "入库数量未填写，无法入库");
        if (finishWarehousing.getWarehousingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "入库数量必须大于 0");
        }

        materialStockService.inboundStock(
                finishWarehousing.getMaterialId(),
                finishWarehousing.getWarehouseId(),
                finishWarehousing.getWarehousingQuantity(),
                "FINISH_WAREHOUSING",
                finishWarehousing.getWarehousingNo(),
                "成品入库"
        );

        // 累计入库量达到生产订单计划数量则自动完成该生产订单
        Long productionOrderId = finishWarehousing.getProductionOrderId();
        if (productionOrderId != null) {
            ProductionOrder productionOrder = productionOrderService.getById(productionOrderId);
            if (productionOrder != null
                    && productionOrder.getStatus() == ProductionOrderStatus.IN_PROGRESS
                    && productionOrder.getPlannedQuantity() != null) {
                BigDecimal inboundTotal = getTotalWarehousingQuantityByOrder(productionOrderId);
                if (inboundTotal.compareTo(productionOrder.getPlannedQuantity()) >= 0) {
                    productionOrderService.completeProductionOrder(productionOrderId);
                }
            }
        }

        return Boolean.TRUE;
    }

    @Override
    public BigDecimal getTotalWarehousingQuantityByOrder(Long productionOrderId) {
        if (productionOrderId == null) {
            return BigDecimal.ZERO;
        }
        List<FinishWarehousing> list = this.list(
                new LambdaQueryWrapper<FinishWarehousing>()
                        .eq(FinishWarehousing::getProductionOrderId, productionOrderId)
                        .eq(FinishWarehousing::getStatus, FinishWarehousingStatus.WAREHOUSED));
        return list.stream()
                .map(FinishWarehousing::getWarehousingQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Boolean cancelFinishWarehousing(Long id) {
        changeFinishWarehousingStatus(
                id,
                FinishWarehousingStatus.DRAFT,
                FinishWarehousingStatus.CANCEL
        );
        return Boolean.TRUE;
    }
}
