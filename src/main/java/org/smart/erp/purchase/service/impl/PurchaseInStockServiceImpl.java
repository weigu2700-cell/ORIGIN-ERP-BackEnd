package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.mapper.PurchaseInStockMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseInStockServiceImpl
    extends ServiceImpl<PurchaseInStockMapper, PurchaseInStock>
    implements PurchaseInStockService
{
}
