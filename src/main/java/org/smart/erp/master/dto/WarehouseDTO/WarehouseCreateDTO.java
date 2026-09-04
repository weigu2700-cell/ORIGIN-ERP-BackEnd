package org.smart.erp.master.dto.WarehouseDTO;

import lombok.Data;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.enums.WarehouseType;

@Data
public class WarehouseCreateDTO {

    private String name;

    private WarehouseType type;

    /** 新增时可显式指定状态，未传时由服务端默认为启用。 */
    private WarehouseStatus status;

    private Long factoryId;

    private String address;

    private String remark;


}
