package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.MaterialDTO.MaterialCreateDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialListDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialUpdateDTO;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.vo.MaterialVO;

public interface MaterialService extends IService<Material> {
    void createMaterial(MaterialCreateDTO dto);

    Page<MaterialVO> listMaterial(MaterialListDTO dto);

    MaterialVO getMaterialDetail(Long id);

    void updateMaterial(Long id, MaterialUpdateDTO dto);

    void changeMaterialStatus(Long id, MaterialStatus status);
}
