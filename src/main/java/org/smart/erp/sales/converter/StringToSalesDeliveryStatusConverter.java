package org.smart.erp.sales.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToSalesDeliveryStatusConverter extends AbstractCodeEnumConverter<SalesDeliveryStatus> {
    public StringToSalesDeliveryStatusConverter() {
        super(SalesDeliveryStatus.class);
    }
}
