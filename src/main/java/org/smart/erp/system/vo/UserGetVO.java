package org.smart.erp.system.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserGetVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String deptName;

    private List<Map<String,Object>> roles;
}
