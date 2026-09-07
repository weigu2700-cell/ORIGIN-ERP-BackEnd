package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderServiceImpl
    extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
    implements PurchaseOrderService
{
}
