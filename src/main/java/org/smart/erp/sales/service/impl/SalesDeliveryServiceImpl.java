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
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.mapper.SalesDeliveryMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesDeliveryItemService;
import org.smart.erp.sales.service.SalesDeliveryService;
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
    private final SalesDeliveryItemService salesDeliveryItemService;
    private final BusinessNoGenerator businessNoGenerator;
    private final CustomerMapper customerMapper;

    public SalesDeliveryServiceImpl(
            SalesDeliveryMapper salesDeliveryMapper,
            SalesDeliveryItemService salesDeliveryItemService,
            BusinessNoGenerator businessNoGenerator,
            CustomerMapper customerMapper,
            SalesOrderMapper salesOrderMapper
    )
    {
        this.salesDeliveryMapper = salesDeliveryMapper;
        this.salesDeliveryItemService = salesDeliveryItemService;
        this.businessNoGenerator = businessNoGenerator;
        this.customerMapper = customerMapper;
        this.salesOrderMapper = salesOrderMapper;
    }


    private Customer getCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404,"客户不存在");
        }

        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDeliveryVo createSalesDeliveryVo(CreateDto dto) {
        // 校验客户存在（@NotNull 只保证非空，不保证有效）
        Customer customer = getCustomer(dto.getCustomerId());

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(400, "发货单明细项不能为空");
        }

        if (salesOrderMapper.selectById(dto.getSalesOrderId()) == null) {
             throw new BusinessException(400, "销售订单不存在");
        }

        SalesDelivery salesDelivery = new SalesDelivery();
        BeanUtils.copyProperties(dto, salesDelivery);
        salesDelivery.setDeliveryNo(
                businessNoGenerator.generateNo("erp:sequence:sales-delivery:", "SD")
        );
        if (!salesDelivery.getId().equals(dto.getSalesOrderId())) {
            throw new BusinessException(400,"发货单与销售订单不一致");
        }
        salesDelivery.setSalesOrderId(dto.getSalesOrderId());
        salesDelivery.setStatus(SalesDeliveryStatus.DRAFT);
        save(salesDelivery);

        List<CreateItemDto> salesDeliveryItemList = dto.getItems();
        int lineNo = 10;
        List<SalesDeliveryItemVo> salesDeliveryItemVoList = new ArrayList<>();
        for (CreateItemDto salesDeliveryItemDto : salesDeliveryItemList) {
            if (!Objects.equals(salesDeliveryItemDto.getDeliveryId(), dto.getSalesOrderId())) {
                throw new BusinessException(400, "发货单明细项与销售订单不一致");
            }
            salesDeliveryItemDto.setDeliveryId(salesDelivery.getId());
            salesDeliveryItemDto.setLineNo(lineNo);
            salesDeliveryItemVoList.add(salesDeliveryItemService.createSalesDeliveryItemVo(salesDeliveryItemDto));
            lineNo += 10;
        }

        SalesDeliveryVo salesDeliveryVo = new SalesDeliveryVo();

        BeanUtils.copyProperties(salesDelivery, salesDeliveryVo);
        // 与列表接口保持一致的填充程度
        salesDeliveryVo.setCustomerName(customer.getName());
        salesDeliveryVo.setItems(salesDeliveryItemVoList);

        return salesDeliveryVo;
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
