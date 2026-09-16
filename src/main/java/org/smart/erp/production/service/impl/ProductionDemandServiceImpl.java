package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.ProductionDemandAddDto;
import org.smart.erp.production.dto.ProductionOrderAddDto;
import org.smart.erp.production.dto.ProductionDemandPageDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;
import org.smart.erp.production.mapper.ProductionDemandMapper;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.vo.ProductionDemandVo;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductionDemandServiceImpl
    extends ServiceImpl<ProductionDemandMapper, ProductionDemand>
        implements ProductionDemandService
{

    private final SalesOrderMapper salesOrderMapper;
    private final MaterialMapper materialMapper;
    private final BusinessNoGenerator businessNoGenerator;
    private final ProductionOrderService productionOrderService;
    private final ProductionDemandMapper productionDemandMapper;

    public ProductionDemandServiceImpl(
            SalesOrderMapper salesOrderMapper,
            MaterialMapper materialMapper,
            BusinessNoGenerator businessNoGenerator,
            ProductionOrderService productionOrderService,
            ProductionDemandMapper productionDemandMapper
    )
    {
        this.salesOrderMapper = salesOrderMapper;
        this.materialMapper = materialMapper;
        this.businessNoGenerator = businessNoGenerator;
        this.productionOrderService = productionOrderService;
        this.productionDemandMapper = productionDemandMapper;
    }

    @Override
    @Transactional
    public void addProductionDemand(ProductionDemandAddDto dto) {
        if (dto.getSourceNo() == null) {
            throw new BusinessException(400, "来源单据号不能为空");
        }
        if (dto.getSourceType() == null) {
            throw new BusinessException(400, "来源单据类型不能为空");
        }
        if (dto.getMaterialId() == null) {
            throw new BusinessException(400, "生产物料不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "需求数量必须大于0");
        }

        if (dto.getSourceType() == ProductionSourceType.SALES_ORDER && dto.getWarehouseId() == null) {
            throw new BusinessException(400, "销售生产需求必须指定目标仓库");
        }

        // 自动销售需求按来源单+物料+目标仓库保持幂等；上游已在单张出货单内汇总相同库存维度的缺口。
        boolean exists = productionDemandMapper.selectCount(
                new LambdaQueryWrapper<ProductionDemand>()
                .eq(ProductionDemand::getSourceType, dto.getSourceType())
                .eq(ProductionDemand::getSourceNo, dto.getSourceNo())
                .eq(ProductionDemand::getMaterialId, dto.getMaterialId())
                .eq(dto.getWarehouseId() != null, ProductionDemand::getWarehouseId, dto.getWarehouseId())
                .isNull(dto.getWarehouseId() == null, ProductionDemand::getWarehouseId)) > 0;
        if (exists) {
            return;
        }

        SalesOrder salesOrder = salesOrderMapper.selectOne(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getOrderNo, dto.getSourceNo())
        );
        if (salesOrder == null) {
            throw new BusinessException(404, "来源单据不存在");
        }

        // 保存需求单
        ProductionDemand productionDemand = new ProductionDemand();
        BeanUtils.copyProperties(dto, productionDemand);
        productionDemand.setDemandNo(
                businessNoGenerator.generateNo("erp:sequence:production-demand:", "PD"));
        productionDemand.setStatus(ProductionStatus.PENDING);
        productionDemandMapper.insert(productionDemand);

        // ProductionDemand → 生成成品生产订单（草稿态，下达时再算 BOM 净需求）
        ProductionOrderAddDto orderDto = new ProductionOrderAddDto();
        orderDto.setMaterialId(dto.getMaterialId());
        orderDto.setWarehouseId(dto.getWarehouseId());
        orderDto.setPlannedQuantity(dto.getQuantity());
        orderDto.setProductionDemandId(productionDemand.getId());
        productionOrderService.addProductionOrder(orderDto);

        productionDemand.setStatus(ProductionStatus.PLANNED);
        productionDemandMapper.updateById(productionDemand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBySalesOrder(String salesOrderNo) {
        if (!StringUtils.hasText(salesOrderNo)) {
            throw new BusinessException(400, "销售订单号不能为空");
        }

        List<ProductionDemand> demands = productionDemandMapper.selectList(
                new LambdaQueryWrapper<ProductionDemand>()
                        .eq(ProductionDemand::getSourceType, ProductionSourceType.SALES_ORDER)
                        .eq(ProductionDemand::getSourceNo, salesOrderNo)
                        .ne(ProductionDemand::getStatus, ProductionStatus.CANCELLED));

        for (ProductionDemand demand : demands) {
            List<ProductionOrder> orders = productionOrderService.list(
                    new LambdaQueryWrapper<ProductionOrder>()
                            .eq(ProductionOrder::getProductionDemandId, demand.getId()));
            for (ProductionOrder order : orders) {
                if (order.getStatus() == ProductionOrderStatus.DRAFT) {
                    productionOrderService.cancelProductionOrder(order.getId());
                } else if (order.getStatus() != ProductionOrderStatus.CANCELLED) {
                    throw new BusinessException(400,
                            "生产订单[" + order.getProductionOrderNo() + "]已进入生产流程，销售订单不可直接取消");
                }
            }
            demand.setStatus(ProductionStatus.CANCELLED);
            productionDemandMapper.updateById(demand);
        }
    }

    @Override
    public Page<ProductionDemandVo> pageProductionDemand(ProductionDemandPageDto dto) {
        LambdaQueryWrapper<ProductionDemand> queryWrapper =
                new LambdaQueryWrapper<ProductionDemand>()
                        .eq(StringUtils.hasText(dto.getDemandNo()),
                                ProductionDemand::getDemandNo, dto.getDemandNo())

                        .eq(Objects.nonNull(dto.getSourceType()),
                                ProductionDemand::getSourceType, dto.getSourceType())

                        .eq(Objects.nonNull(dto.getMaterialId()),
                                ProductionDemand::getMaterialId, dto.getMaterialId())

                        .eq(Objects.nonNull(dto.getStatus()),
                                ProductionDemand::getStatus, dto.getStatus())

                        .orderBy(true, false, ProductionDemand::getCreateTime);

        Page<ProductionDemand> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);

        Set<Long> materialIds = page.getRecords().stream()
                .map(ProductionDemand::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Map.of()
                : materialMapper.selectByIds(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        List<ProductionDemandVo> voList = page.getRecords().stream().map(demand -> {
            ProductionDemandVo vo = new ProductionDemandVo();
            BeanUtils.copyProperties(demand, vo);
            Material material = materialMap.get(demand.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }
            return vo;
        }).toList();

        Page<ProductionDemandVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ProductionDemandVo DetailProductionDemand(Long id) {
        if (id == null) {
            throw new BusinessException(400, "生产需求ID不能为空");
        }
        ProductionDemand demand = baseMapper.selectById(id);
        if (Objects.isNull(demand)) {
            throw new BusinessException(404, "生产需求不存在");
        }

        ProductionDemandVo vo = new ProductionDemandVo();
        BeanUtils.copyProperties(demand, vo);

        Material material = materialMapper.selectById(demand.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getCode());
            vo.setMaterialName(material.getName());
        }

        return vo;
    }
}
