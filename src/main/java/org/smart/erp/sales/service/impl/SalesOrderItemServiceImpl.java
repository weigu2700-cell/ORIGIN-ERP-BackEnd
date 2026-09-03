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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /**
     * 获取物料信息
     * @param materialId 物料ID
     * @return 物料信息
     */
    private Material getMaterial(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getStatus() != MaterialStatus.ENABLE) {
            throw new BusinessException(404,"物料不存在或物料被禁用");
        }
        return material;
    }

    /**
     * 获取仓库信息
     * @param warehouseId 仓库ID
     * @return 仓库信息
     */
    private String getWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || warehouse.getStatus() != WarehouseStatus.ENABLE) {
            throw new BusinessException(404,"仓库不存在或者仓库状态不为启用");
        }
        return warehouse.getName();
    }

    /**
     * 明细实体转 VO，并补齐物料名称/编码、仓库名称。
     * 物料或仓库在映射中不存在时对应字段留空，不抛异常（避免历史脏数据导致整单查询失败）。
     */
    private SalesOrderItemVo toVo(
            SalesOrderItem salesOrderItem,
            Map<Long, Material> materialMap,
            Map<Long, Warehouse> warehouseMap)
    {
        SalesOrderItemVo salesOrderItemVo = new SalesOrderItemVo();
        BeanUtils.copyProperties(salesOrderItem, salesOrderItemVo);

        Material material = materialMap.get(salesOrderItem.getMaterialId());
        if (material != null) {
            salesOrderItemVo.setMaterialName(material.getName());
            salesOrderItemVo.setMaterialCode(material.getCode());
        }
        Warehouse warehouse = warehouseMap.get(salesOrderItem.getWarehouseId());
        if (warehouse != null) {
            salesOrderItemVo.setWarehouseName(warehouse.getName());
        }
        return salesOrderItemVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderItemVo createItem(SalesOrderItem dto) {

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
        BeanUtils.copyProperties(dto, salesOrderItem);
        // 金额以数量 × 单价为准，避免调用方漏算或算错
        salesOrderItem.setAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
        salesOrderItemMapper.insert(salesOrderItem);

        SalesOrderItemVo salesOrderItemVo = new SalesOrderItemVo();
        BeanUtils.copyProperties(salesOrderItem, salesOrderItemVo);
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

        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectByIds(materialIds)
                        .stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectByIds(warehouseIds)
                        .stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return salesOrderItems.stream()
                .map(salesOrderItem -> toVo(salesOrderItem, materialMap, warehouseMap))
                .toList();
    }


    /**
     * 更新明细
     * @param salesOrderId 销售订单ID
     * @param items 明细列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemBySalesOrderId(Long salesOrderId, List<updateItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(400, "销售订单明细项不能为空");
        }

        List<SalesOrderItem> existingItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getSalesOrderId, salesOrderId)
        );

        // 按 id 建索引（重复 id 取首个），避免循环内嵌套遍历
        Map<Long, updateItemDto> itemMap = items.stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getId() != null)
                .collect(Collectors.toMap(updateItemDto::getId, Function.identity(), (a, b) -> a));

        Set<Long> existingIds = existingItems.stream()
                .map(SalesOrderItem::getId)
                .collect(Collectors.toSet());

        // 传入了但库中不存在：可能已被删除，或属于其他销售订单
        List<Long> unknownIds = itemMap.keySet().stream()
                .filter(id -> !existingIds.contains(id))
                .sorted()
                .toList();
        if (!unknownIds.isEmpty()) {
            throw new BusinessException(400, "销售订单明细项不存在，id：" + unknownIds);
        }

        // DTO 的注解校验仅在 Controller 层生效，内部调用需自行兜底（新增行没有 id，按序号定位）
        for (int i = 0; i < items.size(); i++) {
            updateItemDto dto = items.get(i);
            if (dto == null) {
                throw new BusinessException(400, "第 " + (i + 1) + " 条明细不能为空");
            }
            if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "第 " + (i + 1) + " 条明细的数量必须大于 0");
            }
            if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "第 " + (i + 1) + " 条明细的单价必须大于 0");
            }
        }

        // 校验全部通过后才落库：先更新/删除存量行
        for (SalesOrderItem existingItem : existingItems) {
            updateItemDto dto = itemMap.get(existingItem.getId());
            if (dto == null) {
                // 新明细中已不存在 -> 删除该行
                salesOrderItemMapper.deleteById(existingItem.getId());
                continue;
            }

            BeanUtils.copyProperties(dto, existingItem);
            existingItem.setAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
            salesOrderItemMapper.updateById(existingItem);
        }

        // 再插入新增行：行号从当前最大行号往后顺延，步长 10
        int nextLineNo = existingItems.stream()
                .map(SalesOrderItem::getLineNo)
                .max(Integer::compareTo)
                .orElse(0) + 10;

        for (updateItemDto dto : items) {
            if (dto.getId() != null) {
                continue;
            }
            SalesOrderItem newItem = new SalesOrderItem();
            BeanUtils.copyProperties(dto, newItem);
            newItem.setId(null);
            newItem.setSalesOrderId(salesOrderId);
            newItem.setLineNo(nextLineNo);
            // 复用 createItem：内部会校验物料/仓库存在且启用，并重算金额
            createItem(newItem);
            nextLineNo += 10;
        }
    }

    @Override
    public void removeItemBySalesOrderId(Long id) {
        salesOrderItemMapper.delete(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getSalesOrderId, id)
        );
    }

}
