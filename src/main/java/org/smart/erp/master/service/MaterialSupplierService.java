package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierAddDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierPageDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierUpdateDto;
import org.smart.erp.master.entity.MaterialSupplier;
import org.smart.erp.master.vo.MaterialSupplierVo;

import java.util.List;

public interface MaterialSupplierService extends IService<MaterialSupplier> {
    void addMaterialSupplier(MaterialSupplierAddDto dto);

    Page<MaterialSupplierVo> pageMaterialSupplier(MaterialSupplierPageDto dto);

    MaterialSupplierVo getMaterialSupplier(Long id);

    void updateMaterialSupplier(Long id, MaterialSupplierUpdateDto dto);

    void changeMaterialSupplierStatus(Long id);

    void changeMaterialSupplierPreferred(Long materialId , Long supplierId);

    void exportMaterialSupplier(List<Long> ids, HttpServletResponse response);
}
