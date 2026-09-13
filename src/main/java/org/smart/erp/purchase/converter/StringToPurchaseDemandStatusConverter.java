package org.smart.erp.purchase.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToPurchaseDemandStatusConverter extends AbstractCodeEnumConverter<PurchaseDemandStatus> {
    public StringToPurchaseDemandStatusConverter() {
        super(PurchaseDemandStatus.class);
    }
}
