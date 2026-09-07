package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.CreatePurchaseOrderDto;
import org.smart.erp.purchase.dto.PagePurchaseOrderDto;
import org.smart.erp.purchase.dto.UpdatePurchaseOrderDto;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

public interface PurchaseOrderService extends IService<PurchaseOrder> {

    void createPurchaseOrder(CreatePurchaseOrderDto dto);

    void createPurchaseOrderFromDemand(Long purchaseDemandId);

    Page<PurchaseOrderVo> pagePurchaseOrder(PagePurchaseOrderDto dto);

    PurchaseOrderVo detailPurchaseOrder(Long id);

    void updatePurchaseOrder(Long id, UpdatePurchaseOrderDto dto);

    void approvePurchaseOrder(Long id);

    void shipPurchaseOrder(Long id);

    void receivePurchaseOrder(Long id);

    void closePurchaseOrder(Long id);
}
