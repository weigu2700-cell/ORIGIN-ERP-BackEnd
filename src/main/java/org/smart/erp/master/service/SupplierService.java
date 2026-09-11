package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.SupplierDto.SupplierAddDto;
import org.smart.erp.master.dto.SupplierDto.SupplierPageDto;
import org.smart.erp.master.dto.SupplierDto.SupplierUpdateDto;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.vo.SupplierVo;

import java.util.List;

public interface SupplierService extends IService<Supplier> {
    void addSupplier(SupplierAddDto dto);

    Page<SupplierVo> pageSupplier(SupplierPageDto dto);

    SupplierVo detailSupplier(Long id);

    void updateSupplier(Long id, SupplierUpdateDto dto);

    void changeSupplierStatus(Long id, SupplierStatus status);

    void exportSupplier(List<Long> ids, HttpServletResponse response);
}
