package org.smart.erp.inventory.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.inventory.enums.FinishWarehousingStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToFinishWarehousingStatusConverter extends AbstractCodeEnumConverter<FinishWarehousingStatus> {
    public StringToFinishWarehousingStatusConverter() {
        super(FinishWarehousingStatus.class);
    }
}
