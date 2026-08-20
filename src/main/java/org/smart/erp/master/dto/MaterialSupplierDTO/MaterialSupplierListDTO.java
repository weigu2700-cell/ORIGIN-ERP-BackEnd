package org.smart.erp.master.dto.MaterialSupplierDTO;

import lombok.Data;
import org.smart.erp.master.enums.MaterialSupplierStatus;

@Data
public class MaterialSupplierListDTO {

    private Integer page;

    private Integer pageSize;

    private String materialSupplierCode;

    private Long materialId;

    private Long supplierId;

    private MaterialSupplierStatus status;

    private Integer preferred;
}


