package org.smart.erp.production.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.production.enums.ProductionReportStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToProductionReportStatusConverter extends AbstractCodeEnumConverter<ProductionReportStatus> {
    public StringToProductionReportStatusConverter() {
        super(ProductionReportStatus.class);
    }
}
