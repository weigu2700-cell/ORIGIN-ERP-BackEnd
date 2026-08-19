package org.smart.erp.master.dto.WorkshopDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkshopCreateDTO {

    @NotBlank(message = "车间名称不能为空")
    private String name;

    @NotBlank(message = "工厂不能为空")
    private Long factoryId;

    private String shortName;

    private String remark;
}
