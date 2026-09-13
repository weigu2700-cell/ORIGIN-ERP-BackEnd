package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.production.dto.ProductionReportAddDto;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.production.dto.ProductionReportPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionReport;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionReportStatus;
import org.smart.erp.production.event.ProductionReportFinishedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.smart.erp.production.mapper.ProductionOrderMapper;
import org.smart.erp.production.mapper.ProductionReportMapper;
import org.smart.erp.production.service.ProductionReportService;
import org.smart.erp.production.vo.ProductionReportVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductionReportServiceImpl
    extends ServiceImpl<ProductionReportMapper, ProductionReport>
        implements ProductionReportService
{

    private final ProductionOrderMapper productionOrderMapper;
    private final BusinessNoGenerator businessNoGenerator;
    private final CurrentUser currentUser;
    private final MaterialMapper materialMapper;
    private final UserMapper userMapper;
    private final WarehouseMapper warehouseMapper;
    private final ApplicationEventPublisher eventPublisher;


    public ProductionReportServiceImpl(
            ProductionOrderMapper productionOrderMapper,
            BusinessNoGenerator businessNoGenerator,
            CurrentUser currentUser,
            MaterialMapper materialMapper,
            UserMapper userMapper,
            WarehouseMapper warehouseMapper,
            ApplicationEventPublisher eventPublisher
    )
    {
        this.productionOrderMapper = productionOrderMapper;
        this.businessNoGenerator = businessNoGenerator;
        this.currentUser = currentUser;
        this.materialMapper = materialMapper;
        this.userMapper = userMapper;
        this.warehouseMapper = warehouseMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 修改报工单状态
     * @param id 报工单ID
     * @param requireCheckStatus 需要检查的状态
     * @param targetStatus 目标状态
     */
    private void changeReportStatus(
            Long id,
            ProductionReportStatus requireCheckStatus,
            ProductionReportStatus targetStatus
    ) {

        ProductionReport productionReport = getById(id);
        Optional.ofNullable(productionReport)
                .orElseThrow(() -> new BusinessException(404, "生产报工单不存在"));

        ProductionOrder productionOrder =
                productionOrderMapper.selectById(productionReport.getProductionOrderId());
        Optional.ofNullable(productionOrder)
                .orElseThrow(() -> new BusinessException(404, "生产订单不存在"));

        if (!requireCheckStatus.equals(productionReport.getStatus())) {
            throw new BusinessException(400, "生产报工状态不正确");
        }

        productionReport.setStatus(targetStatus);
        boolean updated = updateById(productionReport);
        if (!updated) {
            throw new BusinessException(409, "数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProductionReport(ProductionReportAddDto dto) {
        // P0: 报工数量合法性校验
        BigDecimal reportQty = getReportQty(dto);

        // 关联生产订单状态与物料校验
        ProductionOrder order = productionOrderMapper.selectById(dto.getProductionOrderId());
        if (order == null) {
            throw new BusinessException(404, "生产订单不存在");
        }
        if (order.getStatus() != ProductionOrderStatus.RELEASED
                && order.getStatus() != ProductionOrderStatus.IN_PROGRESS) {
            throw new BusinessException(400, "生产订单未下达或已结束，无法报工");
        }
        if (!Objects.equals(order.getMaterialId(), dto.getMaterialId())) {
            throw new BusinessException(400, "报工物料与生产订单物料不一致");
        }

        // P0: 累计报工不能超过生产计划
        BigDecimal reportedTotal = sumReportQuantity(order.getId());
        if (reportedTotal.add(reportQty).compareTo(order.getPlannedQuantity()) > 0) {
            throw new BusinessException(400, "累计报工数量（" + reportedTotal.add(reportQty)
                    + "）已超过生产计划数量（" + order.getPlannedQuantity() + "）");
        }

        ProductionReport productionReport = new ProductionReport();
        BeanUtils.copyProperties(dto, productionReport);
        productionReport.setProductionReportNo(
                businessNoGenerator.generateNo("erp:sequence:production-report:", "PR"));
        productionReport.setStatus(ProductionReportStatus.DRAFT);
        productionReport.setReportUserId(currentUser.getUserId());
        productionReport.setReportTime(
                Optional.ofNullable(dto.getReportTime())
                        .orElse(LocalDateTime.now())
        );
        save(productionReport);
    }

    private static @NonNull BigDecimal getReportQty(ProductionReportAddDto dto) {
        BigDecimal reportQty = dto.getReportQuantity();
        BigDecimal qualifiedQty = dto.getQualifiedQuantity();
        BigDecimal scrappedQty = dto.getScrappedQuantity();
        if (reportQty == null || reportQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "报工数量必须大于0");
        }
        if (qualifiedQty == null || qualifiedQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "合格数量不能为空且不能为负");
        }
        if (scrappedQty == null || scrappedQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "报废数量不能为空且不能为负");
        }
        if (qualifiedQty.add(scrappedQty).compareTo(reportQty) > 0) {
            throw new BusinessException(400, "合格数量与报废数量之和不能超过报工数量");
        }
        return reportQty;
    }

    /** 统计生产订单已提交报工（排除取消/驳回）的累计报工数量 */
    private BigDecimal sumReportQuantity(Long productionOrderId) {
        List<ProductionReport> reports = this.list(
                new LambdaQueryWrapper<ProductionReport>()
                        .eq(ProductionReport::getProductionOrderId, productionOrderId)
                        .notIn(ProductionReport::getStatus,
                                ProductionReportStatus.CANCEL, ProductionReportStatus.REJECT));
        return reports.stream()
                .map(ProductionReport::getReportQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Page<ProductionReportVo> pageProductionReport(ProductionReportPageDto dto) {
        LambdaQueryWrapper<ProductionReport> qw =
                new LambdaQueryWrapper<ProductionReport>()
                        .like(Objects.nonNull(dto.getProductionReportNo()),
                                ProductionReport::getProductionReportNo, dto.getProductionReportNo())
                        .eq(Objects.nonNull(dto.getProductionOrderId()),
                                ProductionReport::getProductionOrderId, dto.getProductionOrderId())
                        .eq(Objects.nonNull(dto.getMaterialId()),
                                ProductionReport::getMaterialId, dto.getMaterialId())
                        .eq(Objects.nonNull(dto.getStatus()),
                                ProductionReport::getStatus, dto.getStatus())
                        .eq(Objects.nonNull(dto.getReportUserId()),
                                ProductionReport::getReportUserId, dto.getReportUserId())
                        .ge(Objects.nonNull(dto.getReportTime()),
                                ProductionReport::getReportTime, dto.getReportTime());

        Page<ProductionReport> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), qw);

        Set<Long> productionOrderIds = page.getRecords().stream()
                .map(ProductionReport::getProductionOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> materialIds = page.getRecords().stream()
                .map(ProductionReport::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> reportUserIds = page.getRecords().stream()
                .map(ProductionReport::getReportUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> warehouseIds = page.getRecords().stream()
                .map(ProductionReport::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductionOrder> productionOrderMap = productionOrderIds.isEmpty()
                ? Map.of()
                : productionOrderMapper
                    .selectByIds(productionOrderIds)
                    .stream()
                    .collect(Collectors.toMap(ProductionOrder::getId, Function.identity()));

        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Map.of()
                : materialMapper
                    .selectByIds(materialIds)
                    .stream()
                    .collect(Collectors.toMap(Material::getId, Function.identity()));

        Map<Long, User> userMap = reportUserIds.isEmpty()
                ? Map.of()
                : userMapper
                    .selectByIds(reportUserIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty()
                ? Map.of()
                : warehouseMapper
                    .selectByIds(warehouseIds)
                    .stream()
                    .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        Page<ProductionReportVo> voPage = new Page<>(dto.getPageNum(), dto.getPageSize(), page.getTotal());

        return voPage.setRecords(
                page.getRecords().stream().map( productionReport -> {
                    ProductionReportVo vo = new ProductionReportVo();
                    BeanUtils.copyProperties(productionReport, vo);
                    if (productionOrderMap.containsKey(productionReport.getProductionOrderId())) {
                        vo.setProductionOrderNo(productionOrderMap.get(
                                productionReport.getProductionOrderId()).getProductionOrderNo());
                    }
                    if (materialMap.containsKey(productionReport.getMaterialId())) {
                        vo.setMaterialName(materialMap.get(productionReport.getMaterialId()).getName());
                        vo.setMaterialCode(materialMap.get(productionReport.getMaterialId()).getCode());
                    }
                    Long reportUserId = productionReport.getReportUserId();
                    if (reportUserId != null && userMap.containsKey(reportUserId)) {
                        vo.setReportUserName(userMap.get(reportUserId).getUsername());
                    }
                    if (warehouseMap.containsKey(productionReport.getWarehouseId())) {
                        vo.setWarehouseName(warehouseMap.get(productionReport.getWarehouseId()).getName());
                    }
                    return vo;
                }).collect(Collectors.toList())
        );
    }

    @Override
    public ProductionReportVo detailProductionReport(Long id) {
        ProductionReport productionReport = getById(id);
        Optional.ofNullable(productionReport)
                .orElseThrow(() -> new BusinessException(404,"生产报工不存在"));

        ProductionReportVo vo = new ProductionReportVo();
        BeanUtils.copyProperties(productionReport, vo);

        Long productionOrderId = productionReport.getProductionOrderId();
        Optional.ofNullable(productionOrderId)
                .ifPresent(id1 -> {
                    ProductionOrder order = productionOrderMapper.selectById(id1);
                    if (Objects.nonNull(order)) {
                        vo.setProductionOrderNo(order.getProductionOrderNo());
                    }
                });

        Long materialId = productionReport.getMaterialId();
        Optional.ofNullable(materialId)
                .ifPresent(id1 -> {
                    Material material = materialMapper.selectById(id1);
                    if (Objects.nonNull(material)) {
                        vo.setMaterialName(material.getName());
                        vo.setMaterialCode(material.getCode());
                    }
                });

        Long reportUserId = productionReport.getReportUserId();
        Optional.ofNullable(reportUserId)
                .ifPresent(id1 -> {
                    User user = userMapper.selectById(id1);
                    if (Objects.nonNull(user)) {
                        vo.setReportUserName(user.getUsername());
                    }
                });

        Long warehouseId = productionReport.getWarehouseId();
        Optional.ofNullable(warehouseId)
                .ifPresent(id1 -> {
                    Warehouse warehouse = warehouseMapper.selectById(id1);
                    if (Objects.nonNull(warehouse)) {
                        vo.setWarehouseName(warehouse.getName());
                    }
                });

        return vo;
    }

    @Override
    public void approveProductionReport(Long id) {
        changeReportStatus(
                id,
                ProductionReportStatus.DRAFT,
                ProductionReportStatus.APPROVED
        );
    }

    @Override
    public void cancelProductionReport(Long id) {
        changeReportStatus(
                id,
                ProductionReportStatus.DRAFT,
                ProductionReportStatus.CANCEL
        );
    }

    @Override
    public void rejectProductionReport(Long id) {
        changeReportStatus(
                id,
                ProductionReportStatus.APPROVED,
                ProductionReportStatus.REJECT
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishProductionReport(Long id) {
        ProductionReport productionReport = getById(id);

        BigDecimal qualifiedQuantity = productionReport.getQualifiedQuantity();
        if (qualifiedQuantity == null || qualifiedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(400, "合格数量不能为0");
        }

        // 回写生产订单已完成数量（合格量累计），仅用于进度展示；
        // 生产订单的 COMPLETED 以实际成品入库为准（累计入库达到 plannedQuantity 时由 warehouseFinishWarehousing 置为完成）
        ProductionOrder order = productionOrderMapper.selectById(productionReport.getProductionOrderId());
        if (order != null) {
            BigDecimal completed = order.getCompletedQuantity() == null ? BigDecimal.ZERO : order.getCompletedQuantity();
            order.setCompletedQuantity(completed.add(qualifiedQuantity));
            productionOrderMapper.updateById(order);
        }

        // P0: 报工完成自动生成成品入库单（草稿态），通过事件解耦，由成品入库服务监听处理
        eventPublisher.publishEvent(new ProductionReportFinishedEvent(
                this,
                productionReport.getProductionOrderId(),
                productionReport.getId(),
                productionReport.getMaterialId(),
                productionReport.getWarehouseId(),
                qualifiedQuantity,
                currentUser.getUserId()));

        changeReportStatus(
                id,
                ProductionReportStatus.APPROVED,
                ProductionReportStatus.FINISHED
        );

    }
}
