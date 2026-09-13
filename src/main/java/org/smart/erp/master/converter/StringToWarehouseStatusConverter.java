package org.smart.erp.master.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.master.enums.WarehouseStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToWarehouseStatusConverter extends AbstractCodeEnumConverter<WarehouseStatus> {
    public StringToWarehouseStatusConverter() {
        super(WarehouseStatus.class);
    }
}
