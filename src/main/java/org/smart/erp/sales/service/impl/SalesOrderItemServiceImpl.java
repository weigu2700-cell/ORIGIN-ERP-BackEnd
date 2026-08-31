package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.Valid;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.service.SalesOrderItemService;
import org.smart.erp.sales.vo.SalesOrderItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

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

    private String getMaterial(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getStatus() != MaterialStatus.ENABLE) {
            throw new BusinessException(404,"物料不存在或物料被禁用");
        }
        return material.getName();
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
        String materialName = getMaterial(dto.getMaterialId());
        String warehouseName = getWarehouse(dto.getWarehouseId());

        SalesOrderItem salesOrderItem = new SalesOrderItem();
        BeanUtils.copyProperties(dto,salesOrderItem);
        salesOrderItemMapper.insert(salesOrderItem);

        SalesOrderItemVo salesOrderItemVo = new SalesOrderItemVo();

        BeanUtils.copyProperties(salesOrderItem,salesOrderItemVo);
        salesOrderItemVo.setMaterialName(materialName);
        salesOrderItemVo.setWarehouseName(warehouseName);

        return salesOrderItemVo;
    }


}
