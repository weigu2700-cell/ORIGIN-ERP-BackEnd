package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.CreatePurchaseDemandDto;
import org.smart.erp.purchase.dto.PagePurchaseDemandDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.vo.PurchaseDemandVo;

public interface PurchaseDemandService extends IService<PurchaseDemand> {

    PurchaseDemand createPurchaseDemand(CreatePurchaseDemandDto dto);

    Page<PurchaseDemandVo> pagePurchaseDemand(PagePurchaseDemandDto dto);

    PurchaseDemandVo detailPurchaseDemand(Long id);

    void approvePurchaseDemand(Long id);

    void closePurchaseDemand(Long id);
}
