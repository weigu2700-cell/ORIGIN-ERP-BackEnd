package org.smart.erp.master.dto.MaterialSupplierDto;

import lombok.Data;
import org.smart.erp.master.enums.MaterialSupplierStatus;

@Data
public class MaterialSupplierPageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String materialSupplierCode;

    private Long materialId;

    private Long supplierId;

    private MaterialSupplierStatus status;

    private Integer preferred;
}


