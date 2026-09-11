package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.dto.PurchaseInStockPageDto;
import org.smart.erp.purchase.dto.PurchaseInStockUploadDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.vo.PurchaseInStockVo;

public interface PurchaseInStockService extends IService<PurchaseInStock> {

    void addPurchaseInStock(PurchaseInStockAddDto dto);

    Page<PurchaseInStockVo> getPagePurchaseInStock(PurchaseInStockPageDto queryDto);

    void approvePurchaseInStock(Long id);

    void uploadPurchaseInStock(Long id, PurchaseInStockUploadDto dto);

    PurchaseInStockVo getPurchaseInStock(Long id);
}
