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
import org.smart.erp.sales.dto.salesOrderDto.updateDto;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.mapper.SalesDeliveryMapper;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.service.SalesOrderItemService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderItemVo;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final SalesDeliveryMapper salesDeliveryMapper;
    private final SalesDeliveryService salesDeliveryService;

    public SalesOrderServiceImpl(
            SalesOrderMapper salesOrderMapper,
            SalesOrderItemMapper salesOrderItemMapper,
            CustomerMapper customerMapper,
            SalesOrderItemService salesOrderItemService,
            BusinessNoGenerator businessNoGenerator,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper,
            SalesDeliveryMapper salesDeliveryMapper,
            SalesDeliveryService salesDeliveryService
    )
    {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.customerMapper = customerMapper;
        this.salesOrderItemService = salesOrderItemService;
        this.businessNoGenerator = businessNoGenerator;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.salesDeliveryMapper = salesDeliveryMapper;
        this.salesDeliveryService = salesDeliveryService;
    }
    //业务封装：------------------------------------------------

    /**
     * 销售订单状态流转公共逻辑：查单 -> 校验当前状态 -> 执行副作用 -> 置新状态落库 -> 返回最新视图。
     * 由外层 public 方法的事务保证原子性（同类自调用无需再声明事务）。
     *
     * @param expectedStatus 要求的当前状态
     * @param targetStatus   目标状态
     * @param rejectMsg      当前状态不匹配时的提示语
     * @param beforeUpdate   状态落库前执行的业务副作用（如预占/释放库存）
     */
    private SalesOrderVo changeStatus(
            Long id,
            SalesOrderStatus expectedStatus,
            SalesOrderStatus targetStatus,
            String rejectMsg,
            Runnable beforeUpdate)
    {
        SalesOrder salesOrder = salesOrderMapper.selectById(id);
        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }
        if (salesOrder.getStatus() != expectedStatus) {
            throw new BusinessException(400, rejectMsg);
        }

        beforeUpdate.run();

        salesOrder.setStatus(targetStatus);
        salesOrderMapper.updateById(salesOrder);

        return getSalesOrderVoById(id);
    }

    /** 汇总某销售订单全部明细金额，作为订单总金额 */
    private BigDecimal calculateTotalAmount(Long salesOrderId) {
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getSalesOrderId, salesOrderId)
        );
        return items.stream()
                .map(SalesOrderItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //接口：------------------------------------------------

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
        BigDecimal totalAmount = BigDecimal.ZERO;
        int lineNo = 10;
        for (createItemDto item : items) {
            SalesOrderItem salesOrderItem = new SalesOrderItem();
            // 必须先拷贝明细字段（物料/仓库/数量/单价），否则下游校验与落库都会缺值
            BeanUtils.copyProperties(item, salesOrderItem);
            salesOrderItem.setSalesOrderId(salesOrder.getId());
            salesOrderItem.setLineNo(lineNo);
            salesOrderItem.setAmount(item.getQuantity().multiply(item.getUnitPrice()));

            salesOrderItemVos.add(salesOrderItemService.createItem(salesOrderItem));
            totalAmount = totalAmount.add(salesOrderItem.getAmount());
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
    @Transactional(readOnly = true)
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

        // 按客户 id 反查客户名称（不能用订单 id 去查客户表）
        Set<Long> customerIds = orderList.stream()
                .map(SalesOrder::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerMapper.selectByIds(new ArrayList<>(customerIds)).stream()
                        .filter(customer -> customer.getId() != null)
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

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

        SalesOrder salesOrder = salesOrderMapper.selectById(id);
        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }

        Customer customer = customerMapper.selectById(salesOrder.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        String customerName = customer.getName();

        SalesOrderVo salesOrderVo = new SalesOrderVo();
        BeanUtils.copyProperties(salesOrder,salesOrderVo);
        salesOrderVo.setCustomerName(customerName);
        salesOrderVo.setItems(salesOrderItemService.getItemBySalesOrderId(id));

        return salesOrderVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderVo updateSalesOrderVoById(Long id, updateDto dto) {
        SalesOrder salesOrder = salesOrderMapper.selectById(id);
        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }

        if (!salesOrder.getStatus().equals(SalesOrderStatus.DRAFT)) {
            throw new BusinessException(400, "销售订单状态不允许修改");
        }

        if (StringUtils.hasText(dto.getRemark())) salesOrder.setRemark(dto.getRemark());
        if (dto.getDeliveryDate() != null) salesOrder.setDeliveryDate(dto.getDeliveryDate());
        if (dto.getItems() != null) {
            salesOrderItemService.updateItemBySalesOrderId(id, dto.getItems());
            // 明细的数量/单价可能已变更，需重算订单总金额，否则金额与明细不一致
            salesOrder.setTotalAmount(calculateTotalAmount(id));
        }
        salesOrderMapper.updateById(salesOrder);

        return getSalesOrderVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSalesOrderById(Long id) {
        SalesOrder salesOrder = salesOrderMapper.selectById(id);

        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }

        if (salesOrder.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BusinessException(400, "销售订单状态不允许删除");
        }

        salesOrderItemService.removeItemBySalesOrderId(id);
        salesOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderVo confirmSalesOrderById(Long id, updateDto dto) {

        return changeStatus(
                id,
                SalesOrderStatus.DRAFT,
                SalesOrderStatus.CONFIRMED,
                "销售订单状态不允许确认",
                () -> {
                    salesDeliveryService.createDeliveriesForOrder(id);
                    // 级联确认所有草稿态出货单；预占库存由各出货单在确认时自行决定，订单不再直接操作库存
                    List<SalesDelivery> deliveries = salesDeliveryMapper.selectList(
                            new LambdaQueryWrapper<SalesDelivery>().eq(SalesDelivery::getSalesOrderId, id));
                    for (SalesDelivery d : deliveries) {
                        if (d.getStatus() == SalesDeliveryStatus.DRAFT) {
                            salesDeliveryService.confirmSalesDeliveryById(d.getId());
                        }
                    }
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderVo cancelSalesOrderById(Long id, updateDto dto) {
        return changeStatus(
                id,
                SalesOrderStatus.CONFIRMED,
                SalesOrderStatus.CANCELLED,
                "销售订单状态不允许取消",
                () -> {
                    // 级联取消所有未完成的出货单；出货单为“已确认”时会自行释放预占，订单本身不操作库存
                    List<SalesDelivery> deliveries = salesDeliveryMapper.selectList(
                            new LambdaQueryWrapper<SalesDelivery>().eq(SalesDelivery::getSalesOrderId, id));
                    for (SalesDelivery d : deliveries) {
                        if (d.getStatus() == SalesDeliveryStatus.COMPLETED) {
                            throw new BusinessException(400, "订单存在已完成的发货单，无法取消");
                        }
                        if (d.getStatus() == SalesDeliveryStatus.DRAFT
                                || d.getStatus() == SalesDeliveryStatus.CONFIRMED) {
                            salesDeliveryService.cancelSalesDeliveryById(d.getId());
                        }
                    }
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishSalesOrderById(Long id) {
        SalesOrder salesOrder = salesOrderMapper.selectById(id);
        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }
        // 幂等：已被完成出库联动置为完成时直接返回，避免重复处理
        if (salesOrder.getStatus() == SalesOrderStatus.COMPLETED) {
            return;
        }
        if (salesOrder.getStatus() != SalesOrderStatus.CONFIRMED) {
            throw new BusinessException(400, "销售订单状态不允许完成");
        }
        // 订单完成的前提：所有出货单均已出库完成，否则不允许完成
        List<SalesDelivery> deliveries = salesDeliveryMapper.selectList(
                new LambdaQueryWrapper<SalesDelivery>().eq(SalesDelivery::getSalesOrderId, id));
        for (SalesDelivery d : deliveries) {
            if (d.getStatus() != SalesDeliveryStatus.COMPLETED) {
                throw new BusinessException(400, "存在未出库完成的发货单，无法完成订单");
            }
        }
        // 订单仅作里程碑展示，实际库存由出货单完成出库时扣减，此处不再操作库存
        salesOrder.setStatus(SalesOrderStatus.COMPLETED);
        salesOrderMapper.updateById(salesOrder);
    }

}
