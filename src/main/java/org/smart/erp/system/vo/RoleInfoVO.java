package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.RoleEnum;

import java.util.List;

@Data
public class RoleInfoVO {

    private Long id;

    private String name;

    private String code;

    private Integer sort;

    private RoleEnum status;

    /** 该角色关联的权限 id 列表（权限详情由权限模块提供，此处只返回 id） */
    private List<Long> permissionIds;
}
