package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.DeptDTO;
import org.smart.erp.system.dto.DeptListDTO;
import org.smart.erp.system.dto.DeptUpdateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.vo.DeptTreeVO;
import org.smart.erp.system.vo.DeptVO;

import java.util.List;

public interface DeptService extends IService<Dept> {
    /** 新增部门，仅落库不返回数据 */
    void createDept(DeptDTO dto);

    DeptVO getDeptDetail(Long id);

    Page<DeptVO> listDept(DeptListDTO dto);

    /** 更新部门，仅落库不返回数据 */
    void updateDept(DeptUpdateDTO dto);

    Boolean deleteDept(Long id);

    List<DeptTreeVO> getDeptTree();
}
