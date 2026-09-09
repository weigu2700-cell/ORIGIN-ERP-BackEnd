package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.SupplierDTO.SupplierCreateDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierListDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierUpdateDTO;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.vo.SupplierVO;

import java.util.List;

public interface SupplierService extends IService<Supplier> {
    void createSupplier(SupplierCreateDTO dto);

    Page<SupplierVO> listSupplier(SupplierListDTO dto);

    SupplierVO getSupplierDetail(Long id);

    void updateSupplier(Long id, SupplierUpdateDTO dto);

    void changeSupplierStatus(Long id, SupplierStatus status);

    void exportSupplier(List<Long> ids, HttpServletResponse response);
}
