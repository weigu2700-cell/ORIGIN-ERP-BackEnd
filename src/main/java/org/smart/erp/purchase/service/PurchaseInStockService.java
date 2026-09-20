package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.dto.PurchaseInStockPageDto;
import org.smart.erp.purchase.dto.PurchaseInStockUploadDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.vo.PurchaseInStockVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PurchaseInStockService extends IService<PurchaseInStock> {

    void addPurchaseInStock(PurchaseInStockAddDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:in:stock:list')")
    Page<PurchaseInStockVo> getPagePurchaseInStock(PurchaseInStockPageDto queryDto);

    @PreAuthorize("hasAnyAuthority('purchase:in:stock:approve')")
    void approvePurchaseInStock(Long id);

    @PreAuthorize("hasAnyAuthority('purchase:in:stock:upload')")
    void uploadPurchaseInStock(Long id, PurchaseInStockUploadDto dto);

    @PreAuthorize("hasAnyAuthority('purchase:in:stock:detail')")
    PurchaseInStockVo getPurchaseInStock(Long id);
}
