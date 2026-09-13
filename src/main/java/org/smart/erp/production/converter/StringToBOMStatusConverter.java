package org.smart.erp.production.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.production.enums.BOMStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToBOMStatusConverter extends AbstractCodeEnumConverter<BOMStatus> {
    public StringToBOMStatusConverter() {
        super(BOMStatus.class);
    }
}
