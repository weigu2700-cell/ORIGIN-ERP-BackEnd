package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.Valid;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.dto.salesOrderItemDto.updateItemDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.service.SalesOrderItemService;
import org.smart.erp.sales.vo.SalesOrderItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SalesOrderItemServiceImpl
    extends ServiceImpl<SalesOrderItemMapper, SalesOrderItem>
    implements SalesOrderItemService
{

    private final SalesOrderItemMapper salesOrderItemMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    public SalesOrderItemServiceImpl(
            SalesOrderItemMapper salesOrderItemMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper
    )
    {
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
    }

    private Material getMaterial(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getStatus() != MaterialStatus.ENABLE) {
            throw new BusinessException(404,"物料不存在或物料被禁用");
        }
        return material;
    }

    private String getWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || warehouse.getStatus() != WarehouseStatus.ENABLE) {
            throw new BusinessException(404,"仓库不存在或者仓库状态不为启用");
        }
        return warehouse.getName();
    }

    @Override
    @Valid
    public SalesOrderItemVo createItem(@RequestBody @Validated SalesOrderItem dto) {

        if (dto.getSalesOrderId() == null) {
            throw new BusinessException(400,"销售订单ID不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400,"数量不能为空且不能小于0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400,"单价不能为空且不能小于0");
        }
        Material material = getMaterial(dto.getMaterialId());
        String warehouseName = getWarehouse(dto.getWarehouseId());

        SalesOrderItem salesOrderItem = new SalesOrderItem();
        BeanUtils.copyProperties(dto,salesOrderItem);
        salesOrderItemMapper.insert(salesOrderItem);

        SalesOrderItemVo salesOrderItemVo = new SalesOrderItemVo();

        BeanUtils.copyProperties(salesOrderItem,salesOrderItemVo);
        salesOrderItemVo.setMaterialName(material.getName());
        salesOrderItemVo.setMaterialCode(material.getCode());
        salesOrderItemVo.setWarehouseName(warehouseName);

        return salesOrderItemVo;
    }

    @Override
    public List<SalesOrderItemVo> getItemBySalesOrderId(Long salesOrderId) {

        List<SalesOrderItem> salesOrderItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getSalesOrderId, salesOrderId)
        );

        List<Long> materialIds = salesOrderItems.stream().map(SalesOrderItem::getMaterialId).distinct().toList();
        List<Long> warehouseIds = salesOrderItems.stream().map(SalesOrderItem::getWarehouseId).distinct().toList();

        Map<Long, Material> materialMap = materialMapper.selectByIds(materialIds)
                .stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseMapper.selectByIds(warehouseIds)
                .stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return salesOrderItems.stream().map(salesOrderItem -> {
            SalesOrderItemVo salesOrderItemVo = new SalesOrderItemVo();
            BeanUtils.copyProperties(salesOrderItem,salesOrderItemVo);
            salesOrderItemVo.setMaterialName(materialMap.get(salesOrderItem.getMaterialId()).getName());
            salesOrderItemVo.setMaterialCode(materialMap.get(salesOrderItem.getMaterialId()).getCode());
            salesOrderItemVo.setWarehouseName(warehouseMap.get(salesOrderItem.getWarehouseId()).getName());
            return salesOrderItemVo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemBySalesOrderId(Long salesOrderId, List<updateItemDto> items) {

        List<SalesOrderItem> salesOrderItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getSalesOrderId, salesOrderId)
        );

        SalesOrderItem updateItem =  salesOrderItems.stream()
                .filter(Objects::nonNull)
                .peek(salesOrderItem -> {
                    updateItemDto item = items.stream()
                            .filter(i -> i.getId().equals(salesOrderItem.getId()))
                            .findFirst()
                            .orElse(null);
                    if (item == null) {
                        salesOrderItemMapper.deleteById(salesOrderItem.getId());
                    } else {
                        BeanUtils.copyProperties(item,salesOrderItem);
                        salesOrderItemMapper.updateById(salesOrderItem);
                    }
                }).findFirst().orElse(null);
    }

}
