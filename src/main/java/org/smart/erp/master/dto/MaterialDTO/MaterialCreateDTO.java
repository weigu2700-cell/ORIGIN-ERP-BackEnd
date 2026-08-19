package org.smart.erp.master.dto.MaterialDTO;

import lombok.Data;
import org.smart.erp.master.enums.MaterialType;

import java.math.BigDecimal;

@Data
public class MaterialCreateDTO {

    private String name;

    private String unit;

    private String spec;

    private MaterialType type;

    private BigDecimal safetyStock;

    private String remark;

}
