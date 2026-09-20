package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.SupplierDto.SupplierAddDto;
import org.smart.erp.master.dto.SupplierDto.SupplierPageDto;
import org.smart.erp.master.dto.SupplierDto.SupplierUpdateDto;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.vo.SupplierVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface SupplierService extends IService<Supplier> {
    @PreAuthorize("hasAuthority('master:supplier:create')")
    void addSupplier(SupplierAddDto dto);

    @PreAuthorize("hasAuthority('master:supplier:list')")
    Page<SupplierVo> pageSupplier(SupplierPageDto dto);

    @PreAuthorize("hasAuthority('master:supplier:get')")
    SupplierVo detailSupplier(Long id);

    @PreAuthorize("hasAuthority('master:supplier:update')")
    void updateSupplier(Long id, SupplierUpdateDto dto);

    @PreAuthorize("hasAuthority('master:supplier:status')")
    void changeSupplierStatus(Long id, SupplierStatus status);

    @PreAuthorize("hasAuthority('master:supplier:export')")
    void exportSupplier(List<Long> ids, HttpServletResponse response);
}
