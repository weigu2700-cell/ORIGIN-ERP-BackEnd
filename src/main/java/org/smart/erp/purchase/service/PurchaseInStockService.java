package org.smart.erp.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.purchase.dto.CreatePurchaseInStockDto;
import org.smart.erp.purchase.entity.PurchaseInStock;

public interface PurchaseInStockService extends IService<PurchaseInStock> {

    void createPurchaseInStock(CreatePurchaseInStockDto dto);
}
