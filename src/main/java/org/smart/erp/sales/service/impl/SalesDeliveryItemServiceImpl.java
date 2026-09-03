package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.sales.dto.salesDeliveryItemDto.CreateItemDto;
import org.smart.erp.sales.entity.SalesDeliveryItem;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.mapper.SalesDeliveryItemMapper;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.service.SalesDeliveryItemService;
import org.smart.erp.sales.vo.SalesDeliveryItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SalesDeliveryItemServiceImpl
    extends ServiceImpl<SalesDeliveryItemMapper, SalesDeliveryItem>
    implements SalesDeliveryItemService

{

    private final SalesDeliveryItemMapper salesDeliveryItemMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;

    public SalesDeliveryItemServiceImpl(
            SalesDeliveryItemMapper salesDeliveryItemMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper,
            SalesOrderItemMapper salesOrderItemMapper
    )
    {
        this.salesDeliveryItemMapper = salesDeliveryItemMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
    }

    /**
     * 获取物料信息，不存在或未启用时抛业务异常
     * @param materialId 物料ID
     * @return 物料信息
     */
    private Material getMaterial(Long materialId) {
        if (materialId == null) {
            throw new BusinessException(400, "物料不能为空");
        }
        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getStatus() != MaterialStatus.ENABLE) {
            throw new BusinessException(404, "物料不存在或物料被禁用");
        }
        return material;
    }

    /**
     * 获取仓库信息，不存在或未启用时抛业务异常
     * @param warehouseId 仓库ID
     * @return 仓库信息
     */
    private Warehouse getWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new BusinessException(400, "仓库不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || warehouse.getStatus() != WarehouseStatus.ENABLE) {
            throw new BusinessException(404, "仓库不存在或者仓库状态不为启用");
        }
        return warehouse;
    }





    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryItemVo createSalesDeliveryItemVo(CreateItemDto dto, Long deliveryId, Integer lineNo) {
        if (dto.getSalesOrderItemId() == null) {
            throw new BusinessException(400, "销售订单明细ID不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "发货数量必须大于 0");
        }
        if (deliveryId == null) {
            throw new BusinessException(400, "发货单ID不能为空");
        }

        // 出库明细必须能对应到一条有效的销售订单明细，并从中带出物料/仓库
        SalesOrderItem orderItem = salesOrderItemMapper.selectById(dto.getSalesOrderItemId());
        if (orderItem == null) {
            throw new BusinessException(404, "销售订单明细不存在");
        }

        Material material = getMaterial(orderItem.getMaterialId());
        Warehouse warehouse = getWarehouse(orderItem.getWarehouseId());

        SalesDeliveryItem salesDeliveryItem = new SalesDeliveryItem();
        salesDeliveryItem.setDeliveryId(deliveryId);
        salesDeliveryItem.setLineNo(lineNo);
        salesDeliveryItem.setSalesOrderItemId(orderItem.getId());
        salesDeliveryItem.setMaterialId(orderItem.getMaterialId());
        salesDeliveryItem.setWarehouseId(orderItem.getWarehouseId());
        salesDeliveryItem.setQuantity(dto.getQuantity());
        save(salesDeliveryItem);

        SalesDeliveryItemVo salesDeliveryItemVo = new SalesDeliveryItemVo();
        BeanUtils.copyProperties(salesDeliveryItem, salesDeliveryItemVo);
        salesDeliveryItemVo.setMaterialName(material.getName());
        salesDeliveryItemVo.setMaterialCode(material.getCode());
        salesDeliveryItemVo.setWarehouseName(warehouse.getName());
        return salesDeliveryItemVo;
    }

    @Override
    public List<SalesDeliveryItemVo> getItemVoByDeliveryIds(Collection<Long> deliveryIds) {
        if (deliveryIds == null || deliveryIds.isEmpty()) {
            return List.of();
        }

        List<SalesDeliveryItem> items = salesDeliveryItemMapper.selectList(
                new LambdaQueryWrapper<SalesDeliveryItem>()
                        .in(SalesDeliveryItem::getDeliveryId, deliveryIds)
                        .orderByAsc(SalesDeliveryItem::getLineNo)
        );
        if (items.isEmpty()) {
            return List.of();
        }

        Set<Long> materialIds = items.stream()
                .map(SalesDeliveryItem::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> warehouseIds = items.stream()
                .map(SalesDeliveryItem::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Collections.emptyMap()
                : materialMapper.selectByIds(new ArrayList<>(materialIds)).stream()
                        .collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseMapper.selectByIds(new ArrayList<>(warehouseIds)).stream()
                        .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return items.stream().map(item -> {
            SalesDeliveryItemVo vo = new SalesDeliveryItemVo();
            BeanUtils.copyProperties(item, vo);

            Material material = materialMap.get(item.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getName());
                vo.setMaterialCode(material.getCode());
            }
            Warehouse warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            return vo;
        }).toList();
    }
}
