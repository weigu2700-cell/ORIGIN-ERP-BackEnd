package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.dto.salesOrderDto.listDto;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesOrderItemService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderItemVo;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SalesOrderServiceImpl
        extends ServiceImpl<SalesOrderMapper, SalesOrder>
        implements SalesOrderService
{

    private  final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final SalesOrderItemService salesOrderItemService;
    private final CustomerMapper customerMapper;
    private final BusinessNoGenerator businessNoGenerator;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    public SalesOrderServiceImpl(
            SalesOrderMapper salesOrderMapper,
            SalesOrderItemMapper salesOrderItemMapper,
            CustomerMapper customerMapper,
            SalesOrderItemService salesOrderItemService,
            BusinessNoGenerator businessNoGenerator,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper
    )
    {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.customerMapper = customerMapper;
        this.salesOrderItemService = salesOrderItemService;
        this.businessNoGenerator = businessNoGenerator;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @PreAuthorize("hasAnyAuthority('sales:order:create')")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderVo create(createDto dto) {
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404,"客户不存在");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(400,"销售订单明细项不能为空");
        }

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(dto.getCustomerId());
        salesOrder.setStatus(SalesOrderStatus.DRAFT);
        salesOrder.setOrderNo(businessNoGenerator.generateOrderNo());
        salesOrder.setOrderDate(LocalDateTime.now());
        salesOrder.setDeliveryDate(dto.getDeliveryDate());
        salesOrder.setRemark(dto.getRemark());
        salesOrderMapper.insert(salesOrder);

        List<createItemDto> items = dto.getItems();


        List<SalesOrderItemVo> salesOrderItemVos = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal(0);
        int lineNo = 10;
        for (createItemDto item : items) {
            SalesOrderItem salesOrderItem = new SalesOrderItem();
            salesOrderItem.setSalesOrderId(salesOrder.getId());
            salesOrderItem.setLineNo(lineNo);
            salesOrderItem.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            salesOrderItemVos.add(salesOrderItemService.createItem(salesOrderItem));
            totalAmount = totalAmount.add(item.getQuantity().multiply(item.getUnitPrice()));
            lineNo += 10;
        }

        salesOrder.setTotalAmount(totalAmount);
        salesOrderMapper.updateById(salesOrder);

        SalesOrderVo salesOrderVo = new SalesOrderVo();
        String customerName = customer.getName();
        BeanUtils.copyProperties(salesOrder,salesOrderVo);
        salesOrderVo.setCustomerName(customerName);
        salesOrderVo.setItems(salesOrderItemVos);
        return salesOrderVo;
    }

    @Override
    public Page<SalesOrderVo> listSalesOrderVoByPage(listDto dto) {

        LambdaQueryWrapper<SalesOrder> queryWrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(dto.getCustomerId() != null, SalesOrder::getCustomerId, dto.getCustomerId())
                .like(dto.getOrderNo() != null, SalesOrder::getOrderNo, dto.getOrderNo())
                .eq(dto.getStatus() != null, SalesOrder::getStatus, dto.getStatus())
                .ge(dto.getOrderDate() != null, SalesOrder::getOrderDate, dto.getOrderDate());

        Page<SalesOrder> orderPage = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);
        List<SalesOrder> orderList = orderPage.getRecords();

        // 若当前页无数据，直接返回空的分页结果（避免后续空集合查询）
        if (orderList.isEmpty()) {
            Page<SalesOrderVo> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize());
            voPage.setTotal(orderPage.getTotal());
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Long> orderIds = orderList.stream().map(SalesOrder::getId).collect(Collectors.toList());

        Map<Long, String> customerNameMap = customerMapper.selectByIds(orderIds)
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<SalesOrderItem> allItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .in(!orderIds.isEmpty(), SalesOrderItem::getSalesOrderId, orderIds)
        );

        Map<Long, List<SalesOrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(SalesOrderItem::getSalesOrderId));

        Set<Long> materialIds = allItems.stream().map(SalesOrderItem::getMaterialId).collect(Collectors.toSet());
        Set<Long> warehouseIds = allItems.stream().map(SalesOrderItem::getWarehouseId).collect(Collectors.toSet());

        Map<Long, Material> materialMap;
        Map<Long, Warehouse> warehouseMap;

        if (!materialIds.isEmpty()) {
            materialMap = materialMapper.selectByIds(new ArrayList<>(materialIds))
                    .stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        } else {
            materialMap = Collections.emptyMap();
        }
        if (!warehouseIds.isEmpty()) {
            warehouseMap = warehouseMapper.selectByIds(new ArrayList<>(warehouseIds))
                    .stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        } else {
            warehouseMap = Collections.emptyMap();
        }

        List<SalesOrderVo> voList = orderList.stream().map(order -> {
            SalesOrderVo vo = new SalesOrderVo();
            BeanUtils.copyProperties(order, vo);
            vo.setCustomerName(customerNameMap.get(order.getCustomerId()));

            List<SalesOrderItem> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            List<SalesOrderItemVo> itemVos = items.stream().map(item -> {
                SalesOrderItemVo itemVo = new SalesOrderItemVo();
                BeanUtils.copyProperties(item, itemVo);
                Material material = materialMap.get(item.getMaterialId());
                if (material != null) {
                    itemVo.setMaterialName(material.getName());
                    itemVo.setMaterialCode(material.getCode());
                }
                Warehouse warehouse = warehouseMap.get(item.getWarehouseId());
                if (warehouse != null) {
                    itemVo.setWarehouseName(warehouse.getName());
                }
                return itemVo;
            }).collect(Collectors.toList());
            vo.setItems(itemVos);
            return vo;
        }).collect(Collectors.toList());

        Page<SalesOrderVo> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize());
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public SalesOrderVo getSalesOrderVoById(Long id) {
        return null;
    }

}
