package org.smart.erp.master.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.master.enums.MaterialSupplierStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToMaterialSupplierStatusConverter extends AbstractCodeEnumConverter<MaterialSupplierStatus> {
    public StringToMaterialSupplierStatusConverter() {
        super(MaterialSupplierStatus.class);
    }
}
