package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.sales.dto.salesDeliveryDto.CreateDto;
import org.smart.erp.sales.dto.salesDeliveryDto.ListDto;
import org.smart.erp.sales.dto.salesDeliveryItemDto.CreateItemDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.entity.SalesDeliveryItem;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.mapper.SalesDeliveryMapper;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesDeliveryItemService;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesDeliveryItemVo;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class SalesDeliveryServiceImpl
        extends ServiceImpl<SalesDeliveryMapper, SalesDelivery>
        implements SalesDeliveryService
{

    private final SalesDeliveryMapper salesDeliveryMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final SalesDeliveryItemService salesDeliveryItemService;
    private final MaterialStockService materialStockService;
    private final BusinessNoGenerator businessNoGenerator;
    private final CustomerMapper customerMapper;
    private final SalesOrderService salesOrderService;

    public SalesDeliveryServiceImpl(
            SalesDeliveryMapper salesDeliveryMapper,
            SalesDeliveryItemService salesDeliveryItemService,
            MaterialStockService materialStockService,
            BusinessNoGenerator businessNoGenerator,
            CustomerMapper customerMapper,
            SalesOrderMapper salesOrderMapper,
            SalesOrderItemMapper salesOrderItemMapper,
            SalesOrderService salesOrderService
    )
    {
        this.salesDeliveryMapper = salesDeliveryMapper;
        this.salesDeliveryItemService = salesDeliveryItemService;
        this.materialStockService = materialStockService;
        this.businessNoGenerator = businessNoGenerator;
        this.customerMapper = customerMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.salesOrderService = salesOrderService;
    }

    //业务方法:--------------------------------------------------

    /**
     * 校验客户存在
     * @param customerId
     * @return
     */
    private Customer getCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404,"客户不存在");
        }

        return customer;
    }

    /**
     * 状态流转公共逻辑：查单 -> 校验当前状态 -> 执行副作用 -> 置新状态 -> 返回视图。
     * @param id 发货单ID
     * @param expected 期望状态
     * @param target 目标状态
     * @param rejectMsg 状态不匹配时返回的错误信息
     * @param beforeUpdate 更新前执行的副作用
     * @return 发货单视图
     */
    private SalesDeliveryVo changeStatus(Long id,
                                         SalesDeliveryStatus expected,
                                         SalesDeliveryStatus target,
                                         String rejectMsg,
                                         Runnable beforeUpdate) {
        SalesDelivery delivery = salesDeliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(404, "发货单不存在");
        }
        if (delivery.getStatus() != expected) {
            throw new BusinessException(400, rejectMsg);
        }
        beforeUpdate.run();
        delivery.setStatus(target);
        salesDeliveryMapper.updateById(delivery);
        return getSalesDeliveryVoById(id);
    }

    /**
     * 完成出库：逐行扣减实际库存（在库与预占同步减少），任一行不足则整体回滚
     */
    private void outboundStockForDelivery(Long deliveryId) {
        List<SalesDeliveryItemVo> items = salesDeliveryItemService.getItemVoByDeliveryIds(List.of(deliveryId));
        if (items.isEmpty()) {
            throw new BusinessException(400, "发货单明细项不能为空，无法确认出库");
        }
        for (SalesDeliveryItemVo item : items) {
            materialStockService.outboundStock(
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getQuantity()
            );
        }
    }

    /** 确认发货时逐行预占库存（可用库存不足则整体回滚）；预占由出货单决定 */
    private void reserveStockForDelivery(Long deliveryId) {
        List<SalesDeliveryItemVo> items = salesDeliveryItemService.getItemVoByDeliveryIds(List.of(deliveryId));
        if (items.isEmpty()) {
            throw new BusinessException(400, "发货单明细项不能为空，无法预占库存");
        }
        for (SalesDeliveryItemVo item : items) {
            materialStockService.reserveStock(
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getQuantity()
            );
        }
    }

    /** 取消发货时释放已预占库存（仅“已确认”发货单曾预占）；草稿态无需处理 */
    private void releaseStockForDelivery(Long deliveryId) {
        List<SalesDeliveryItemVo> items = salesDeliveryItemService.getItemVoByDeliveryIds(List.of(deliveryId));
        for (SalesDeliveryItemVo item : items) {
            materialStockService.releaseStock(
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getQuantity()
            );
        }
    }

    //接口实现:--------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo createSalesDeliveryVo(CreateDto dto) {
        // 销售订单必须存在，客户/订单号等信息从订单带出
        SalesOrder salesOrder = salesOrderMapper.selectById(dto.getSalesOrderId());
        if (salesOrder == null) {
            throw new BusinessException(400, "销售订单不存在");
        }
        // 只有已确认的销售订单才允许创建发货单
        if (salesOrder.getStatus() != SalesOrderStatus.CONFIRMED) {
            throw new BusinessException(400, "仅已确认的销售订单可创建发货单");
        }
        Customer customer = getCustomer(salesOrder.getCustomerId());

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(400, "发货单明细项不能为空");
        }

        SalesDelivery salesDelivery = new SalesDelivery();
        BeanUtils.copyProperties(dto, salesDelivery);
        salesDelivery.setDeliveryNo(
                businessNoGenerator.generateNo("erp:sequence:sales-delivery:", "SD")
        );
        salesDelivery.setSalesOrderId(dto.getSalesOrderId());
        salesDelivery.setSalesOrderNo(salesOrder.getOrderNo());
        salesDelivery.setCustomerId(customer.getId());
        salesDelivery.setStatus(SalesDeliveryStatus.DRAFT);
        save(salesDelivery);

        // 批量加载销售订单明细，校验归属同一订单并避免逐条查询
        List<Long> orderItemIds = dto.getItems().stream()
                .map(CreateItemDto::getSalesOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SalesOrderItem> orderItemMap = orderItemIds.isEmpty() ? Collections.emptyMap()
                : salesOrderItemMapper.selectByIds(orderItemIds).stream()
                    .collect(Collectors.toMap(SalesOrderItem::getId, item -> item));

        // 已发货量（用于校验剩余可出货量，防止超发）
        Map<Long, BigDecimal> deliveredQtyMap = salesDeliveryItemService.sumDeliveredQuantityByOrderItemIds(orderItemIds);

        int lineNo = 10;
        List<SalesDeliveryItemVo> itemVoList = new ArrayList<>();
        for (CreateItemDto itemDto : dto.getItems()) {
            SalesOrderItem orderItem = orderItemMap.get(itemDto.getSalesOrderItemId());
            if (orderItem == null || !Objects.equals(orderItem.getSalesOrderId(), dto.getSalesOrderId())) {
                throw new BusinessException(400, "发货明细关联的销售订单明细不存在或不属于该销售订单");
            }
            // 剩余可出货量 = 订单明细数量 - 已发货量，必须 >= 本次发货数量
            BigDecimal alreadyDelivered = deliveredQtyMap.getOrDefault(orderItem.getId(), BigDecimal.ZERO);
            BigDecimal remaining = orderItem.getQuantity().subtract(alreadyDelivered);
            if (itemDto.getQuantity().compareTo(remaining) > 0) {
                throw new BusinessException(400, "发货数量超出可出货量：订单明细 " + orderItem.getId()
                        + " 已发货 " + alreadyDelivered + "，剩余可发货 " + remaining);
            }
            itemVoList.add(salesDeliveryItemService.createSalesDeliveryItemVo(itemDto, salesDelivery.getId(), lineNo));
            lineNo += 10;
        }

        SalesDeliveryVo salesDeliveryVo = new SalesDeliveryVo();
        BeanUtils.copyProperties(salesDelivery, salesDeliveryVo);
        // 与列表接口保持一致的填充程度
        salesDeliveryVo.setCustomerName(customer.getName());
        salesDeliveryVo.setItems(itemVoList);

        return salesDeliveryVo;
    }

    /**
     * 创建销售订单时按仓库批量生成草稿态出货单：每个仓库一张出货单，包含该仓库的全部订单明细行。
     * 出货单为 DRAFT，预留库存仍由订单确认时统一处理；与订单创建同事务，订单创建失败则一并回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDeliveriesForOrder(Long salesOrderId) {
        SalesOrder salesOrder = salesOrderMapper.selectById(salesOrderId);
        if (salesOrder == null) {
            throw new BusinessException(404, "销售订单不存在");
        }
        List<SalesOrderItem> orderItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getSalesOrderId, salesOrderId)
        );
        if (orderItems.isEmpty()) {
            return;
        }
        // 按仓库分组：每个仓库生成一张草稿态出货单（无仓库的明细归入 -1 组，避免空键 NPE）
        Map<Long, List<SalesOrderItem>> byWarehouse = orderItems.stream()
                .collect(Collectors.groupingBy(item -> item.getWarehouseId() == null ? -1L : item.getWarehouseId()));

        List<SalesDeliveryItem> allDeliveryItems = new ArrayList<>();
        for (Map.Entry<Long, List<SalesOrderItem>> entry : byWarehouse.entrySet()) {
            SalesDelivery delivery = new SalesDelivery();
            delivery.setDeliveryNo(businessNoGenerator.generateNo("erp:sequence:sales-delivery:", "SD"));
            delivery.setSalesOrderId(salesOrder.getId());
            delivery.setSalesOrderNo(salesOrder.getOrderNo());
            delivery.setCustomerId(salesOrder.getCustomerId());
            delivery.setStatus(SalesDeliveryStatus.DRAFT);
            salesDeliveryMapper.insert(delivery);

            int lineNo = 10;
            for (SalesOrderItem item : entry.getValue()) {
                SalesDeliveryItem di = new SalesDeliveryItem();
                di.setDeliveryId(delivery.getId());
                di.setLineNo(lineNo);
                di.setSalesOrderItemId(item.getId());
                di.setMaterialId(item.getMaterialId());
                di.setWarehouseId(item.getWarehouseId());
                di.setQuantity(item.getQuantity());
                allDeliveryItems.add(di);
                lineNo += 10;
            }
        }
        if (!allDeliveryItems.isEmpty()) {
            salesDeliveryItemService.saveBatch(allDeliveryItems);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDeliveryVo getSalesDeliveryVoById(Long id) {
        SalesDelivery delivery = salesDeliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(404, "发货单不存在");
        }
        SalesDeliveryVo vo = new SalesDeliveryVo();
        BeanUtils.copyProperties(delivery, vo);
        vo.setItems(salesDeliveryItemService.getItemVoByDeliveryIds(List.of(id)));
        if (delivery.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(delivery.getCustomerId());
            vo.setCustomerName(customer != null ? customer.getName() : null);
        }
        return vo;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo confirmSalesDeliveryById(Long id) {
        // 确认即预占库存（由出货单决定）；实际扣减延迟至“完成出库”时执行
        return changeStatus(id,
                SalesDeliveryStatus.DRAFT,
                SalesDeliveryStatus.CONFIRMED,
                "发货单状态不允许确认",
                () -> reserveStockForDelivery(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo completeSalesDeliveryById(Long id) {
        SalesDelivery delivery = salesDeliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(404, "发货单不存在");
        }
        // 先完成出库：逐行扣减实际库存（在库与预占同步减少），任一行不足则整体回滚
        SalesDeliveryVo vo = changeStatus(id,
                SalesDeliveryStatus.CONFIRMED,
                SalesDeliveryStatus.COMPLETED,
                "仅已确认的发货单可完成出库",
                () -> outboundStockForDelivery(id));
        // 仅当该订单下所有出货单均已出库完成时，才联动将销售订单置为完成
        if (allDeliveriesCompleted(delivery.getSalesOrderId())) {
            salesOrderService.finishSalesOrderById(delivery.getSalesOrderId());
        }
        return vo;
    }

    /** 该订单下的出货单是否非空且全部出库完成 */
    private boolean allDeliveriesCompleted(Long salesOrderId) {
        List<SalesDelivery> deliveries = salesDeliveryMapper.selectList(
                new LambdaQueryWrapper<SalesDelivery>().eq(SalesDelivery::getSalesOrderId, salesOrderId));
        if (deliveries.isEmpty()) {
            return false;
        }
        for (SalesDelivery d : deliveries) {
            if (d.getStatus() != SalesDeliveryStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo cancelSalesDeliveryById(Long id) {
        SalesDelivery delivery = salesDeliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BusinessException(404, "发货单不存在");
        }
        if (delivery.getStatus() == SalesDeliveryStatus.COMPLETED) {
            throw new BusinessException(400, "已出库完成的发货单不可取消");
        }
        if (delivery.getStatus() == SalesDeliveryStatus.CANCELLED) {
            throw new BusinessException(400, "发货单已取消");
        }
        // 仅“已确认”发货单此前预占了库存，取消时释放预占；草稿态无需处理
        if (delivery.getStatus() == SalesDeliveryStatus.CONFIRMED) {
            releaseStockForDelivery(id);
        }
        delivery.setStatus(SalesDeliveryStatus.CANCELLED);
        salesDeliveryMapper.updateById(delivery);
        return getSalesDeliveryVoById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesDeliveryVo> getPageSalesDeliveryVo(ListDto dto) {
        LambdaQueryWrapper<SalesDelivery> queryWrapper =
                new LambdaQueryWrapper<SalesDelivery>()
                        .eq(dto.getSalesOrderId() != null, SalesDelivery::getSalesOrderId, dto.getSalesOrderId())
                        .like(StringUtils.hasText(dto.getDeliveryNo()), SalesDelivery::getDeliveryNo, dto.getDeliveryNo())
                        .eq(dto.getStatus() != null, SalesDelivery::getStatus, dto.getStatus())
                        .eq(dto.getCustomerId() != null, SalesDelivery::getCustomerId, dto.getCustomerId());

        if (queryWrapper == null){
            return new Page<>();
        }
        Page<SalesDelivery> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);

        Page<SalesDeliveryVo> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(page.getTotal());

        List<SalesDelivery> records = page.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查客户，避免逐条查询（原实现在循环里每条查一次）
        Set<Long> customerIds = records.stream()
                .map(SalesDelivery::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerMapper.selectByIds(new ArrayList<>(customerIds)).stream()
                        .filter(customer -> customer.getId() != null)
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

        // 一次性加载当前页全部明细，按发货单 id 分组，避免逐条查询
        List<Long> deliveryIdList = records.stream()
                .map(SalesDelivery::getId)
                .toList();
        Map<Long, List<SalesDeliveryItemVo>> itemMap = salesDeliveryItemService
                .getItemVoByDeliveryIds(deliveryIdList).stream()
                .collect(Collectors.groupingBy(SalesDeliveryItemVo::getDeliveryId));

        List<SalesDeliveryVo> voList = records.stream()
                .map(salesDelivery -> {
                    SalesDeliveryVo salesDeliveryVo = new SalesDeliveryVo();
                    BeanUtils.copyProperties(salesDelivery, salesDeliveryVo);
                    salesDeliveryVo.setCustomerName(customerNameMap.get(salesDelivery.getCustomerId()));
                    salesDeliveryVo.setItems(
                            itemMap.getOrDefault(salesDelivery.getId(), Collections.emptyList()));
                    return salesDeliveryVo;
                })
                .toList();

        voPage.setRecords(voList);
        return voPage;
    }
}
