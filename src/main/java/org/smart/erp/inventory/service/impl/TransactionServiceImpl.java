package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.mapper.TransactionMapper;
import org.smart.erp.inventory.service.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl
        extends ServiceImpl<TransactionMapper, Transaction>
        implements TransactionService
{

    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Override
    public void recordTransaction
            (MaterialStock stock,
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

        transaction.setBusinessType(Integer.valueOf(businessType));
        transaction.setBusinessNo(businessNo);

        transaction.setQuantity(quantity);

        transaction.setBeforeOnHand(beforeOnHand);
        transaction.setAfterOnHand(stock.getOnHand());

        transaction.setBeforeReserved(beforeReserved);
        transaction.setAfterReserved(stock.getReserved());

        transaction.setRemark(remark);

        transactionMapper.insert(transaction);
    }
}
