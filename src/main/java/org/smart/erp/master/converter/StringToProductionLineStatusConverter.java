package org.smart.erp.master.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToProductionLineStatusConverter extends AbstractCodeEnumConverter<ProductionLineStatus> {
    public StringToProductionLineStatusConverter() {
        super(ProductionLineStatus.class);
    }
}
