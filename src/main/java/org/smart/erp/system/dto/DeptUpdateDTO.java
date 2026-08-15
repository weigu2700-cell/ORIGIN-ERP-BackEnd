package org.smart.erp.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.smart.erp.system.Enum.DeptStatus;

@Data
public class DeptUpdateDTO {

    @NotNull(message = "部门ID不能为空")
    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private DeptStatus status;


}
