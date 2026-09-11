package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.PurchaseOrderAddDto;
import org.smart.erp.purchase.dto.PurchaseOrderPageDto;
import org.smart.erp.purchase.dto.PurchaseOrderUpdateDto;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

public interface PurchaseOrderService extends IService<PurchaseOrder> {

    void addPurchaseOrder(PurchaseOrderAddDto dto);

    void addPurchaseOrderFromDemand(Long purchaseDemandId);

    Page<PurchaseOrderVo> pagePurchaseOrder(PurchaseOrderPageDto dto);

    PurchaseOrderVo detailPurchaseOrder(Long id);

    void updatePurchaseOrder(Long id, PurchaseOrderUpdateDto dto);

    void approvePurchaseOrder(Long id);

    void shipPurchaseOrder(Long id);

    void receivePurchaseOrder(Long id);

    void closePurchaseOrder(Long id);
}
