package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.createProductionOrderDto;
import org.smart.erp.production.dto.pageProductionOrderDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.mapper.ProductionDemandMapper;
import org.smart.erp.production.mapper.ProductionOrderMapper;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ProductionOrderServiceImpl
    extends ServiceImpl<ProductionOrderMapper, ProductionOrder>
    implements ProductionOrderService
{

    private final BusinessNoGenerator businessNoGenerator;
    private final ProductionDemandMapper productionDemandMapper;
    private final MaterialMapper materialMapper;
    private final BOMService bomService;

    public ProductionOrderServiceImpl(
            BusinessNoGenerator businessNoGenerator,
            MaterialMapper materialMapper,
            ProductionDemandMapper productionDemandMapper,
            BOMService bomService
    )
    {
        this.businessNoGenerator = businessNoGenerator;
        this.productionDemandMapper = productionDemandMapper;
        this.materialMapper = materialMapper;
        this.bomService = bomService;
    }

    @Override
    public void createProductionOrder(createProductionOrderDto dto) {
        if (dto.getPlannedQuantity() == null || dto.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "生产数量必须大于0");
        }

        Long materialId = dto.getMaterialId();
        BigDecimal plannedQuantity = dto.getPlannedQuantity();

        // 由生产需求建单：物料与数量以需求为准
        if (dto.getProductionDemandId() != null) {
            ProductionDemand demand = productionDemandMapper.selectById(dto.getProductionDemandId());
            if (demand == null) {
                throw new BusinessException(400, "生产需求不存在");
            }
            materialId = demand.getMaterialId();
            plannedQuantity = demand.getQuantity();
            if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "生产需求数量为空或非法");
            }
        }

        // 校验最终生效的物料存在
        if (materialId == null || materialMapper.selectById(materialId) == null) {
            throw new BusinessException(400, "生产物料不存在");
        }

        ProductionOrder order = new ProductionOrder();
        BeanUtils.copyProperties(dto, order);
        order.setMaterialId(materialId);
        order.setPlannedQuantity(plannedQuantity);
        order.setProductionOrderNo(businessNoGenerator.generateNo("erp:sequence:production-order:", "PO"));
        order.setStatus(ProductionOrderStatus.DRAFT);
        order.setCompletedQuantity(BigDecimal.ZERO);
        save(order);
    }

    @Override
    public Page<ProductionOrderVo> pageProductionOrder(pageProductionOrderDto dto) {

        LambdaQueryWrapper<ProductionOrder> queryWrapper =
                new LambdaQueryWrapper<ProductionOrder>()
                        .eq(Objects.nonNull(dto.getMaterialId()),
                                ProductionOrder::getMaterialId, dto.getMaterialId())

                        .like(StringUtils.hasText(dto.getProductionOrderNo()),
                                ProductionOrder::getProductionOrderNo, dto.getProductionOrderNo())

                        .eq(Objects.nonNull(dto.getStatus()),
                                ProductionOrder::getStatus, dto.getStatus())

                        .ge(Objects.nonNull(dto.getPlannedStartTime()),
                                ProductionOrder::getPlannedStartTime, dto.getPlannedStartTime())

                        .le(Objects.nonNull(dto.getPlannedEndTime()),
                                ProductionOrder::getPlannedEndTime, dto.getPlannedEndTime())

                        .ge(Objects.nonNull(dto.getActualStartTime()),
                                ProductionOrder::getActualStartTime, dto.getActualStartTime())

                        .le(Objects.nonNull(dto.getActualEndTime()),
                                ProductionOrder::getActualEndTime, dto.getActualEndTime())

                        .orderBy(true, false, ProductionOrder::getCreateTime);

        Page<ProductionOrder> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);

        Set<Long> materialIds = page.getRecords().stream()
                .map(ProductionOrder::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> productionDemandIds = page.getRecords().stream()
                .map(ProductionOrder::getProductionDemandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Material> materialMap =
                materialIds.isEmpty()
                        ? Collections.emptyMap()
                        : materialMapper
                        .selectByIds(materialIds)
                        .stream()
                        .collect(Collectors.toMap(Material::getId, m -> m));

        Map<Long, ProductionDemand> productionDemandMap =
                productionDemandIds.isEmpty()
                        ? Collections.emptyMap()
                        : productionDemandMapper
                        .selectByIds(productionDemandIds)
                        .stream()
                        .collect(Collectors.toMap(ProductionDemand::getId, p -> p));

        List<ProductionOrderVo> voList = page.getRecords().stream().map(order -> {

            ProductionOrderVo vo = new ProductionOrderVo();
            BeanUtils.copyProperties(order, vo);

            Material material = materialMap.get(order.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }
            if (Objects.nonNull(order.getProductionDemandId())) {
                ProductionDemand demand = productionDemandMap.get(order.getProductionDemandId());
                if (demand != null) {
                    vo.setProductionDemandNo(demand.getDemandNo());
                }
            }
            return vo;
        }).toList();

        Page<ProductionOrderVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ProductionOrderVo DetailProductionOrder(Long id) {
        if (id == null) {
            throw new BusinessException(400, "生产单ID不能为空");
        }
        ProductionOrder order = baseMapper.selectById(id);
        if (Objects.isNull(order)) {
            throw new BusinessException(404, "生产单不存在");
        }

        ProductionOrderVo vo = new ProductionOrderVo();
        BeanUtils.copyProperties(order, vo);

        Material material = materialMapper.selectById(order.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getCode());
            vo.setMaterialName(material.getName());
        }

        if (Objects.nonNull(order.getProductionDemandId())) {
            ProductionDemand demand = productionDemandMapper.selectById(order.getProductionDemandId());
            if (demand != null) {
                vo.setProductionDemandNo(demand.getDemandNo());
            }
        }
        return vo;
    }

    @Override
    public void startProductionOrder(Long id) {
        transition(id, ProductionOrderStatus.DRAFT, ProductionOrderStatus.IN_PROGRESS,
                "生产单状态不为草稿，无法开始生产",
                o -> o.setActualStartTime(LocalDateTime.now()));
    }

    @Override
    public void completeProductionOrder(Long id) {
        transition(id, ProductionOrderStatus.IN_PROGRESS, ProductionOrderStatus.COMPLETED,
                "生产单状态不为进行中，无法完成生产",
                o -> o.setActualEndTime(LocalDateTime.now()));
    }

    @Override
    public void cancelProductionOrder(Long id) {
        transition(id, ProductionOrderStatus.DRAFT, ProductionOrderStatus.CANCELLED,
                "生产单状态不为草稿，无法取消", null);
    }

    @Override
    public List<MaterialRequirementVo> releaseProductionOrder(Long id) {
        ProductionOrder order = getOrderOrThrow(id);
        if (order.getStatus() != ProductionOrderStatus.DRAFT) {
            throw new BusinessException(400, "生产单状态不为草稿，无法下达");
        }
        order.setStatus(ProductionOrderStatus.RELEASED);
        baseMapper.updateById(order);
        // 下达时按成品 + 计划数量计算 BOM 净需求，仅返回物料需求结果（不建单）
        return bomService.calculateMaterialRequirement(order.getMaterialId(), order.getPlannedQuantity());
    }

    /** 校验状态后执行生产单状态流转；extra 用于设置开始/结束时间等附加字段 */
    private void transition(Long id, ProductionOrderStatus expected, ProductionOrderStatus next,
                            String errorMsg, Consumer<ProductionOrder> extra) {
        ProductionOrder order = getOrderOrThrow(id);
        if (order.getStatus() != expected) {
            throw new BusinessException(400, errorMsg);
        }
        order.setStatus(next);
        if (extra != null) {
            extra.accept(order);
        }
        baseMapper.updateById(order);
    }

    /** 按 ID 加载生产单，ID 为空或记录不存在则抛对应异常 */
    private ProductionOrder getOrderOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException(400, "生产单ID不能为空");
        }
        ProductionOrder order = baseMapper.selectById(id);
        if (Objects.isNull(order)) {
            throw new BusinessException(404, "生产单不存在");
        }
        return order;
    }


}
