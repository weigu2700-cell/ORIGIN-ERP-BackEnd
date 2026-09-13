package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.inventory.dto.FinishWarehousingAddDto;
import org.smart.erp.inventory.dto.FinishWarehousingPageDto;
import org.smart.erp.inventory.entity.FinishWarehousing;
import org.smart.erp.inventory.vo.FinishWarehousingVo;

import java.math.BigDecimal;

public interface FinishWarehousingService extends IService<FinishWarehousing> {

    void addFinishWarehousing(FinishWarehousingAddDto dto);

    Page<FinishWarehousingVo> pageFinishWarehousing(FinishWarehousingPageDto dto);

    FinishWarehousingVo getFinishWarehousing(Long id);

    Boolean approveFinishWarehousing(Long id);

    Boolean warehouseFinishWarehousing(Long id);

    Boolean cancelFinishWarehousing(Long id);

    BigDecimal getTotalWarehousingQuantityByOrder(Long productionOrderId);
}
