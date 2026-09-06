package org.smart.erp.master.dto.ProductionLineDTO;

import lombok.Data;
import org.smart.erp.master.enums.ProductionLineStatus;

@Data
public class ProductionLineListDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private Long workshopId;

    private ProductionLineStatus status;


}
