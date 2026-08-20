package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierCreateDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierListDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierUpdateDTO;
import org.smart.erp.master.entity.MaterialSupplier;
import org.smart.erp.master.vo.MaterialSupplierVO;

public interface MaterialSupplierService extends IService<MaterialSupplier> {
    void createMaterialSupplier(MaterialSupplierCreateDTO dto);

    Page<MaterialSupplierVO> listMaterialSupplier(MaterialSupplierListDTO dto);

    MaterialSupplierVO getMaterialSupplier(Long id);

    void updateMaterialSupplier(Long id, MaterialSupplierUpdateDTO dto);

    void changeMaterialSupplierStatus(Long id);

    void changeMaterialSupplierPreferred(Long materialId , Long supplierId);
}
