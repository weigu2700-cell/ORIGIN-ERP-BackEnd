package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.mapper.TransactionMapper;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.TransactionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl
        extends ServiceImpl<TransactionMapper, Transaction>
        implements TransactionService
{

    private final TransactionMapper transactionMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    public TransactionServiceImpl(
            TransactionMapper transactionMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper)
    {
        this.transactionMapper = transactionMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public void recordTransaction(
            MaterialStock stock,
            TransactionType type,
            BigDecimal quantity,
            BigDecimal beforeOnHand,
            BigDecimal beforeReserved,
            String businessType,
            String businessNo,
            String remark
    )
    {
        Transaction transaction = new Transaction();
        transaction.setWarehouseId(stock.getWarehouseId());
        transaction.setMaterialId(stock.getMaterialId());

        transaction.setTransactionType(type);

        transaction.setBusinessType(businessType);
        transaction.setBusinessNo(businessNo);

        transaction.setQuantity(quantity);

        transaction.setBeforeOnHand(beforeOnHand);
        transaction.setAfterOnHand(stock.getOnHand());

        transaction.setBeforeReserved(beforeReserved);
        transaction.setAfterReserved(stock.getReserved());

        transaction.setRemark(remark);

        transactionMapper.insert(transaction);
    }

    @Override
    public Page<TransactionVO> listTransaction(ListDto listDto) {
        LambdaQueryWrapper<Transaction> queryWrapper =
                new LambdaQueryWrapper<Transaction>()
                        .eq(listDto.getWarehouseId() != null, Transaction::getWarehouseId, listDto.getWarehouseId())
                        .eq(listDto.getMaterialId() != null, Transaction::getMaterialId, listDto.getMaterialId())
                        .eq(StringUtils.hasText(listDto.getBusinessType()), Transaction::getBusinessType, listDto.getBusinessType())
                        .eq(StringUtils.hasText(listDto.getBusinessNo()), Transaction::getBusinessNo, listDto.getBusinessNo());

        Page<Transaction> page = this.page(new Page<>(listDto.getPageNum(), listDto.getPageSize()), queryWrapper);

        List<Long> materialIds = page.getRecords().stream().map(Transaction::getMaterialId).toList();
        List<Long> warehouseIds = page.getRecords().stream().map(Transaction::getWarehouseId).toList();

        Map<Long, Material> materialMap = materialMapper.selectByIds(materialIds).stream()
                .filter(Objects::nonNull).collect(Collectors.toMap(Material::getId, m -> m));
        Map<Long, String> warehouseNameMap = warehouseMapper.selectByIds(warehouseIds).stream()
                .filter(Objects::nonNull).collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));

        return PageConvertUtils.convert(page, transaction -> {
            TransactionVO vo = new TransactionVO();
            BeanUtils.copyProperties(transaction, vo);

            TransactionType type = transaction.getTransactionType();
            if (type != null) {
                vo.setTransactionType(type.getCode());
                vo.setTransactionTypeName(type.getDesc());
            }

            Material material = materialMap.get(transaction.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getName());
                vo.setMaterialCode(material.getCode());
            }
            vo.setWarehouseName(warehouseNameMap.get(transaction.getWarehouseId()));
            return vo;
        });
    }
}
