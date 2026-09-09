package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.purchase.dto.CreatePurchaseInStockDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.smart.erp.purchase.mapper.PurchaseInStockMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseInStockServiceImpl
    extends ServiceImpl<PurchaseInStockMapper, PurchaseInStock>
    implements PurchaseInStockService
{

    private final PurchaseInStockMapper purchaseInStockMapper;
    private final Validator validator;
    private final BusinessNoGenerator businessNoGenerator;

    public PurchaseInStockServiceImpl(PurchaseInStockMapper purchaseInStockMapper,
                                      Validator validator,
                                      BusinessNoGenerator businessNoGenerator) {
        this.purchaseInStockMapper = purchaseInStockMapper;
        this.validator = validator;
        this.businessNoGenerator = businessNoGenerator;
    }

    /**
     * 判断对象是否为空，为空则抛出异常
     * @param obj 对象
     * @param msg 异常信息
     */
    private void checkNull(Object obj, String msg) {
        if (obj == null || (obj instanceof String s && s.isBlank())) {
            throw new BusinessException(400, msg);
        }
    }

    @Override
    public void createPurchaseInStock(CreatePurchaseInStockDto dto) {
        checkNull(dto, "入库信息不能为空");

        // 只校验 DTO 上标注了约束的字段，未标注的（备注、库位、各日期等）允许为空
        Set<ConstraintViolation<CreatePurchaseInStockDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(400, violations.iterator().next().getMessage());
        }

        PurchaseInStock purchaseInStock = new PurchaseInStock();
        BeanUtils.copyProperties(dto, purchaseInStock);
        purchaseInStock.setInStockNo(businessNoGenerator.generateNo("erp:sequence:purchase-in-stock:", "PI"));
        purchaseInStock.setStatus(PurchaseInStockStatus.DRAFT);
        purchaseInStock.setInType(PurchaseInStockType.PURCHASE_NORMAL);
        this.save(purchaseInStock);

    }
}
