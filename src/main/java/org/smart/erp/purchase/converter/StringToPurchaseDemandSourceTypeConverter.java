package org.smart.erp.purchase.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.springframework.stereotype.Component;

@Component
public class StringToPurchaseDemandSourceTypeConverter extends AbstractCodeEnumConverter<PurchaseDemandSourceType> {
    public StringToPurchaseDemandSourceTypeConverter() {
        super(PurchaseDemandSourceType.class);
    }
}
