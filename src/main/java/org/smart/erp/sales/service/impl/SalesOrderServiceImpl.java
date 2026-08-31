package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesOrderServiceImpl
        extends ServiceImpl<SalesOrderMapper, SalesOrder>
        implements SalesOrderService
{

    private  final SalesOrderMapper salesOrderMapper;

    private final SalesOrderItemMapper salesOrderItemMapper;

    private final SalesOrderItemService salesOrderItemService;

    private final CustomerMapper customerMapper;

    public SalesOrderServiceImpl(
            SalesOrderMapper salesOrderMapper,
            SalesOrderItemMapper salesOrderItemMapper,
            CustomerMapper customerMapper,
            SalesOrderItemService salesOrderItemService
    )
    {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.customerMapper = customerMapper;
        this.salesOrderItemService = salesOrderItemService;
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
        salesOrder.setOrderNo("SO" + LocalDateTime.now()
                .format(DateTimeFormatter
                        .ofPattern("yyyyMMddHHmmss")));
        salesOrder.setOrderDate(LocalDateTime.now());
        salesOrder.setDeliveryDate(dto.getDeliveryDate());
        salesOrder.setRemark(dto.getRemark());
        salesOrderMapper.insert(salesOrder);

        List<createItemDto> items = dto.getItems();

        List<SalesOrderItemVo> salesOrderItemVos = new ArrayList<>();
        for (createItemDto item : items) {
            SalesOrderItem salesOrderItem = new SalesOrderItem();
            salesOrderItem.setSalesOrderId(salesOrder.getId());
            salesOrderItem.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            salesOrderItemVos.add(salesOrderItemService.createItem(salesOrderItem));
        }

        BigDecimal totalAmount = new BigDecimal(0);
        for (createItemDto item : items) {
            totalAmount = totalAmount.add(item.getQuantity().multiply(item.getUnitPrice()));
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
}
