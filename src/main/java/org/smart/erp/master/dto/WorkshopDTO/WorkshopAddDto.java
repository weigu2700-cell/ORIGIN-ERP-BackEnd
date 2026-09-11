package org.smart.erp.master.dto.WorkshopDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkshopAddDto {

    @NotBlank(message = "车间名称不能为空")
    private String name;

    @NotBlank(message = "工厂不能为空")
    private Long factoryId;

    private String shortName;

    private String remark;
}
