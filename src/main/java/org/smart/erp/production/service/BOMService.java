package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.BOMAddDto;
import org.smart.erp.production.dto.BOMPageDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.vo.BOMExplosionVo;
import org.smart.erp.production.vo.BOMVo;
import org.smart.erp.production.vo.MaterialRequirementVo;

import java.math.BigDecimal;
import java.util.List;

public interface BOMService extends IService<BOM> {

    void activeBOM(Long id);

    void addBOM(BOMAddDto dto);

    BOMVo detailBOMDetail(Long id);

    Page<BOMVo> getPageBOMVo(BOMPageDto dto);

    void disableBOM(Long id);

    List<BOMExplosionVo> getBOMExplosion(Long materialId, BigDecimal quantity);

    List<MaterialRequirementVo> calculateMaterialRequirement(Long materialId, BigDecimal quantity);
}
