package org.smart.erp.production.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.production.enums.ProductionSourceType;
import org.springframework.stereotype.Component;

@Component
public class StringToProductionSourceTypeConverter extends AbstractCodeEnumConverter<ProductionSourceType> {
    public StringToProductionSourceTypeConverter() {
        super(ProductionSourceType.class);
    }
}
