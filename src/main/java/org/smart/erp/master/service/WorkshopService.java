package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.master.dto.WorkshopDto.WorkshopAddDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopPageDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopUpdateDto;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.vo.WorkshopVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WorkshopService extends IService<Workshop> {
    @PreAuthorize("hasAnyAuthority('master:workshop:create')")
    void addWorkshop(WorkshopAddDto dto);

    @PreAuthorize("hasAnyAuthority('master:workshop:list')")
    Page<WorkshopVo> pageWorkshop(WorkshopPageDto dto);

    @PreAuthorize("hasAnyAuthority('master:workshop:get')")
    WorkshopVo detailWorkshop(Long id);

    @PreAuthorize("hasAnyAuthority('master:workshop:update')")
    void updateWorkshop(Long id, WorkshopUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('master:workshop:status')")
    void changeStatus(Long id, WorkshopStatus status);
}
