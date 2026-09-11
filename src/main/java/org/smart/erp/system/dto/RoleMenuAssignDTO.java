package org.smart.erp.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleMenuAssignDto {

    private Long roleId;

    private List<Long> menuIds;
}
