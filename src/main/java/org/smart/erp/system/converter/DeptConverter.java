package org.smart.erp.system.converter;

import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.vo.DeptVo;

import java.time.LocalDateTime;

public class DeptConverter {

    /** 实体转 Vo（parentName 不设置，由调用方填充） */
    public static DeptVo toVO(Dept dept) {
        return toVO(dept, null);
    }

    /** 实体转 Vo，并指定父部门名称 */
    public static DeptVo toVO(Dept dept, String parentName) {
        if (dept == null) {
            return null;
        }
        DeptVo vo = new DeptVo();
        vo.setId(String.valueOf(dept.getId()));
        vo.setName(dept.getName());
        vo.setCode(dept.getCode());
        vo.setParentName(parentName);
        vo.setCreateTime(format(dept.getCreateTime()));
        vo.setUpdateTime(format(dept.getUpdateTime()));
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time != null ? time.toString() : null;
    }
}
