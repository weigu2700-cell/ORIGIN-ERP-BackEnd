package org.smart.erp.production.service;

import org.smart.erp.production.vo.BOMExplosionVo;
import org.smart.erp.production.vo.MaterialRequirementVo;

import java.math.BigDecimal;
import java.util.List;

/** Internal orchestration port; callers use their own protected use-case permission. */
public interface BOMInternalService {
	List<BOMExplosionVo> getBOMExplosionInternally(Long materialId, BigDecimal quantity);
	List<MaterialRequirementVo> calculateMaterialRequirementInternally(Long materialId, BigDecimal quantity);
}
