package org.smart.erp.system.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.smart.erp.system.Enum.DeptStatus;

@Data
public class DeptListDTO {

    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private int pageSize = 10;

    private String name;

    private String code;

    private Long parentId;

    private DeptStatus status;

    private int sort;
}
