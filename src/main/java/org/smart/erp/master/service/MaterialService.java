package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.MaterialDto.MaterialAddDto;
import org.smart.erp.master.dto.MaterialDto.MaterialPageDto;
import org.smart.erp.master.dto.MaterialDto.MaterialUpdateDto;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.vo.MaterialVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface MaterialService extends IService<Material> {
    @PreAuthorize("hasAnyAuthority('master:material:create')")
    void addMaterial(MaterialAddDto dto);

    @PreAuthorize("hasAnyAuthority('master:material:list')")
    Page<MaterialVo> pageMaterial(MaterialPageDto dto);

    @PreAuthorize("hasAnyAuthority('master:material:get')")
    MaterialVo detailMaterial(Long id);

    @PreAuthorize("hasAnyAuthority('master:material:update')")
    void updateMaterial(Long id, MaterialUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('master:material:status')")
    void changeMaterialStatus(Long id, MaterialStatus status);

    @PreAuthorize("hasAnyAuthority('master:material:status')")
    void toggleMaterialStatus(Long id);

    @PreAuthorize("hasAnyAuthority('master:material:export')")
    void exportMaterial(List<Long> ids, HttpServletResponse response);
}
