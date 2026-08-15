package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.DeptDTO;
import org.smart.erp.system.dto.DeptListDTO;
import org.smart.erp.system.dto.DeptUpdateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.vo.DeptVO;

public interface DeptService extends IService<Dept> {
    DeptVO createDept(DeptDTO dto);

    DeptVO getDeptDetail(Long id);

    Page<DeptVO> getDeptList(DeptListDTO dto);

    DeptVO updateDept(DeptUpdateDTO dto);

    Boolean deleteDept(Long id);
}
