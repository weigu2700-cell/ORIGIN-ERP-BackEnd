package org.smart.erp.master.dto.ProductionLineDto;

import lombok.Data;
import org.smart.erp.master.enums.ProductionLineStatus;

@Data
public class ProductionLinePageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private Long workshopId;

    private ProductionLineStatus status;


}
