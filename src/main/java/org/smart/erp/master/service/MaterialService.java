package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.MaterialDto.MaterialAddDto;
import org.smart.erp.master.dto.MaterialDto.MaterialPageDto;
import org.smart.erp.master.dto.MaterialDto.MaterialUpdateDto;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.vo.MaterialVo;

import java.util.List;

public interface MaterialService extends IService<Material> {
    void addMaterial(MaterialAddDto dto);

    Page<MaterialVo> pageMaterial(MaterialPageDto dto);

    MaterialVo detailMaterial(Long id);

    void updateMaterial(Long id, MaterialUpdateDto dto);

    void changeMaterialStatus(Long id, MaterialStatus status);

    /** 切换物料状态：启用/停用互转 */
    void toggleMaterialStatus(Long id);

    void exportMaterial(List<Long> ids, HttpServletResponse response);
}
