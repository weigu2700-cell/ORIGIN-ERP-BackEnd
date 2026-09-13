package org.smart.erp.production.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToProductionOrderStatusConverter extends AbstractCodeEnumConverter<ProductionOrderStatus> {
    public StringToProductionOrderStatusConverter() {
        super(ProductionOrderStatus.class);
    }
}
