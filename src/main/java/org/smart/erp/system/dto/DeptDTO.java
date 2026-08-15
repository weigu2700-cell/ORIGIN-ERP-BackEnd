package org.smart.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptDTO {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    @NotBlank(message = "部门编码不能为空")
    private String code;

    private Long parentId;

    private int sort;
}
