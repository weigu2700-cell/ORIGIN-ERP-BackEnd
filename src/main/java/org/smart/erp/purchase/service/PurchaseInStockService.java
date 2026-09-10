package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.CreatePurchaseInStockDto;
import org.smart.erp.purchase.dto.PagePurchaseInStockDto;
import org.smart.erp.purchase.dto.UploadPurchaseInStockDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.vo.PurchaseInStockVo;

public interface PurchaseInStockService extends IService<PurchaseInStock> {

    void createPurchaseInStock(CreatePurchaseInStockDto dto);

    Page<PurchaseInStockVo> getPagePurchaseInStock(PagePurchaseInStockDto queryDto);

    void approvePurchaseInStock(Long id);

    void uploadPurchaseInStock(Long id, UploadPurchaseInStockDto dto);
}
