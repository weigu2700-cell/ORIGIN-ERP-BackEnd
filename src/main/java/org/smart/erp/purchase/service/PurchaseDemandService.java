package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.purchase.dto.PurchaseDemandAddDto;
import org.smart.erp.purchase.dto.PurchaseDemandPageDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.vo.PurchaseDemandVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PurchaseDemandService extends IService<PurchaseDemand> {

    @PreAuthorize("hasAnyAuthority('purchase:demand:create')")
    PurchaseDemand addPurchaseDemand(PurchaseDemandAddDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:demand:list')")
    Page<PurchaseDemandVo> pagePurchaseDemand(PurchaseDemandPageDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:demand:get')")
    PurchaseDemandVo detailPurchaseDemand(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:demand:approve')")
    void approvePurchaseDemand(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:demand:close')")
    void closePurchaseDemand(Long id);
}
