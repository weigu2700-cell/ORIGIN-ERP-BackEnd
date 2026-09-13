package org.smart.erp.production.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.production.enums.ProductionStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToProductionStatusConverter extends AbstractCodeEnumConverter<ProductionStatus> {
    public StringToProductionStatusConverter() {
        super(ProductionStatus.class);
    }
}
