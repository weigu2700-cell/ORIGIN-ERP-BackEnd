package org.smart.erp.purchase.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToPurchaseInStockStatusConverter extends AbstractCodeEnumConverter<PurchaseInStockStatus> {
    public StringToPurchaseInStockStatusConverter() {
        super(PurchaseInStockStatus.class);
    }
}
