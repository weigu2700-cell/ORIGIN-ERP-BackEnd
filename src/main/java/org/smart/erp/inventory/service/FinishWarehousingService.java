package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.inventory.dto.FinishWarehousingAddDto;
import org.smart.erp.inventory.dto.FinishWarehousingPageDto;
import org.smart.erp.inventory.entity.FinishWarehousing;
import org.smart.erp.inventory.vo.FinishWarehousingVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;

public interface FinishWarehousingService extends IService<FinishWarehousing> {

    void addFinishWarehousing(FinishWarehousingAddDto dto);

    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:list')")
    Page<FinishWarehousingVo> pageFinishWarehousing(FinishWarehousingPageDto dto);

    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:get')")
    FinishWarehousingVo getFinishWarehousing(Long id);

    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    Boolean approveFinishWarehousing(Long id);

    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    Boolean warehouseFinishWarehousing(Long id);

    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    Boolean cancelFinishWarehousing(Long id);

    BigDecimal getTotalWarehousingQuantityByOrder(Long productionOrderId);
}
