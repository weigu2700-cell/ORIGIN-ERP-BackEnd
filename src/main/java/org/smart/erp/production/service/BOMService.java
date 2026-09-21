package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.production.dto.BOMAddDto;
import org.smart.erp.production.dto.BOMPageDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.vo.BOMExplosionVo;
import org.smart.erp.production.vo.BOMVo;
import org.smart.erp.production.vo.MaterialRequirementVo;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BOMService extends IService<BOM> {

    @PreAuthorize("hasAnyAuthority('production:bom:update')")
    void activeBOM(Long id);

    @PreAuthorize("hasAnyAuthority('production:bom:create')")
    void addBOM(BOMAddDto dto);

    @PreAuthorize("hasAnyAuthority('production:bom:get')")
    BOMVo detailBOMDetail(Long id);

    @PreAuthorize("hasAnyAuthority('production:bom:list')")
    Page<BOMVo> getPageBOMVo(BOMPageDto dto);

    @PreAuthorize("hasAnyAuthority('production:bom:disable')")
    void disableBOM(Long id);

    @PreAuthorize("hasAnyAuthority('production:bom:explosion')")
    List<BOMExplosionVo> getBOMExplosion(Long materialId, BigDecimal quantity);

    @PreAuthorize("hasAnyAuthority('production:bom:requirement')")
    List<MaterialRequirementVo> calculateMaterialRequirement(Long materialId, BigDecimal quantity);
}
