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

    public SalesDeliveryServiceImpl(
            SalesDeliveryMapper salesDeliveryMapper,
            SalesDeliveryItemService salesDeliveryItemService,
            MaterialStockService materialStockService,
            BusinessNoGenerator businessNoGenerator,
            CustomerMapper customerMapper,
            SalesOrderMapper salesOrderMapper,
            SalesOrderItemMapper salesOrderItemMapper
    )
    {
        this.salesDeliveryMapper = salesDeliveryMapper;
        this.salesDeliveryItemService = salesDeliveryItemService;
        this.materialStockService = materialStockService;
        this.businessNoGenerator = businessNoGenerator;
        this.customerMapper = customerMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
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
     * 确认发货：逐行出库扣减库存（在库与预占同步减少），任一行不足则整体回滚
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

        int lineNo = 10;
        List<SalesDeliveryItemVo> itemVoList = new ArrayList<>();
        for (CreateItemDto itemDto : dto.getItems()) {
            SalesOrderItem orderItem = orderItemMap.get(itemDto.getSalesOrderItemId());
            if (orderItem == null || !Objects.equals(orderItem.getSalesOrderId(), dto.getSalesOrderId())) {
                throw new BusinessException(400, "发货明细关联的销售订单明细不存在或不属于该销售订单");
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
        return changeStatus(id,
                SalesDeliveryStatus.DRAFT,
                SalesDeliveryStatus.CONFIRMED,
                "发货单状态不允许确认",
                () -> outboundStockForDelivery(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo cancelSalesDeliveryById(Long id) {
        return changeStatus(id,
                SalesDeliveryStatus.DRAFT,
                SalesDeliveryStatus.CANCELLED,
                "仅草稿态发货单可取消",
                () -> {});
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
