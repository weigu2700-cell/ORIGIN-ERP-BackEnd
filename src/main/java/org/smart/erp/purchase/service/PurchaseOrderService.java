package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.purchase.dto.PurchaseOrderAddDto;
import org.smart.erp.purchase.dto.PurchaseOrderPageDto;
import org.smart.erp.purchase.dto.PurchaseOrderUpdateDto;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.vo.PurchaseOrderVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PurchaseOrderService extends IService<PurchaseOrder> {

    @PreAuthorize("hasAnyAuthority('purchase:order:create')")
    void addPurchaseOrder(PurchaseOrderAddDto dto);

    void addPurchaseOrderFromDemand(Long purchaseDemandId);

    @PreAuthorize("hasAnyAuthority('purchase:order:list')")
    Page<PurchaseOrderVo> pagePurchaseOrder(PurchaseOrderPageDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:order:get')")
    PurchaseOrderVo detailPurchaseOrder(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:order:update')")
    void updatePurchaseOrder(Long id, PurchaseOrderUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:order:approve')")
    void approvePurchaseOrder(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:order:ship')")
    void shipPurchaseOrder(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:order:receive')")
    void receivePurchaseOrder(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:order:close')")
    void closePurchaseOrder(Long id);

    boolean checkPurchaseOrder(Long id);
}
