package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.createProductionDemandDto;
import org.smart.erp.production.dto.createProductionOrderDto;
import org.smart.erp.production.dto.pageProductionDemandDto;
import org.smart.erp.production.entity.ProductionDemand;
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

    public ProductionDemandServiceImpl(
            SalesOrderMapper salesOrderMapper,
            MaterialMapper materialMapper,
            BusinessNoGenerator businessNoGenerator,
            ProductionOrderService productionOrderService
    )
    {
        this.salesOrderMapper = salesOrderMapper;
        this.materialMapper = materialMapper;
        this.businessNoGenerator = businessNoGenerator;
        this.productionOrderService = productionOrderService;
    }

    @Override
    @Transactional
    public void createProductionDemand(createProductionDemandDto dto) {
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
        save(productionDemand);

        // ProductionDemand → 生成成品生产订单（草稿态，下达时再算 BOM 净需求）
        createProductionOrderDto orderDto = new createProductionOrderDto();
        orderDto.setMaterialId(dto.getMaterialId());
        orderDto.setPlannedQuantity(dto.getQuantity());
        orderDto.setProductionDemandId(productionDemand.getId());
        productionOrderService.createProductionOrder(orderDto);
    }

    @Override
    public Page<ProductionDemandVo> pageProductionDemand(pageProductionDemandDto dto) {
        LambdaQueryWrapper<ProductionDemand> queryWrapper =
                new LambdaQueryWrapper<ProductionDemand>()
                        .eq(Objects.nonNull(dto.getDemandNo()),
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
