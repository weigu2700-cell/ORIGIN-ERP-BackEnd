package org.smart.erp.purchase.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToPurchaseOrderStatusConverter extends AbstractCodeEnumConverter<PurchaseOrderStatus> {
    public StringToPurchaseOrderStatusConverter() {
        super(PurchaseOrderStatus.class);
    }
}
