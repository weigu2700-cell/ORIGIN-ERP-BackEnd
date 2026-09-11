package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.DeptDto;
import org.smart.erp.system.dto.DeptListDto;
import org.smart.erp.system.dto.DeptUpdateDto;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.vo.DeptTreeVo;
import org.smart.erp.system.vo.DeptVo;

import java.util.List;

public interface DeptService extends IService<Dept> {
    /** 新增部门，仅落库不返回数据 */
    void addDept(DeptDto dto);

    DeptVo detailDept(Long id);

    Page<DeptVo> pageDept(DeptListDto dto);

    /** 更新部门，仅落库不返回数据 */
    void updateDept(DeptUpdateDto dto);

    Boolean removeDept(Long id);

    List<DeptTreeVo> getDeptTree();
}
