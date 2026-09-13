package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.ProductionReportAddDto;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.production.dto.ProductionReportPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionReport;
import org.smart.erp.production.enums.ProductionReportStatus;
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
    private final MaterialStockService materialStockService;


    public ProductionReportServiceImpl(
            ProductionOrderMapper productionOrderMapper,
            BusinessNoGenerator businessNoGenerator,
            CurrentUser currentUser,
            MaterialMapper materialMapper,
            UserMapper userMapper,
            MaterialStockService materialStockService
    )
    {
        this.productionOrderMapper = productionOrderMapper;
        this.businessNoGenerator = businessNoGenerator;
        this.currentUser = currentUser;
        this.materialMapper = materialMapper;
        this.userMapper = userMapper;
        this.materialStockService = materialStockService;
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

        // TODO: 若合格数量等于生产订单完成数量，需生成成品入库单（暂未实现）

        changeReportStatus(
                id,
                ProductionReportStatus.APPROVED,
                ProductionReportStatus.FINISHED
        );

    }
}
