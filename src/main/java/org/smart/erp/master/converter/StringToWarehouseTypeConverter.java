package org.smart.erp.master.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.master.enums.WarehouseType;
import org.springframework.stereotype.Component;

@Component
public class StringToWarehouseTypeConverter extends AbstractCodeEnumConverter<WarehouseType> {
    public StringToWarehouseTypeConverter() {
        super(WarehouseType.class);
    }
}
