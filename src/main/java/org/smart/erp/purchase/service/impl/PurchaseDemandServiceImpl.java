package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseDemandServiceImpl
    extends ServiceImpl<PurchaseDemandMapper, PurchaseDemand>
    implements PurchaseDemandService
{
}
