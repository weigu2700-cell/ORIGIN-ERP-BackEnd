package org.smart.erp.master.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.WorkshopDto.WorkshopAddDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopPageDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopUpdateDto;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.vo.WorkshopVo;

public interface WorkshopService extends IService<Workshop> {
    void addWorkshop(WorkshopAddDto dto);

    Page<WorkshopVo> pageWorkshop(WorkshopPageDto dto);

    WorkshopVo detailWorkshop(Long id);

    void updateWorkshop(Long id, WorkshopUpdateDto dto);

    void changeStatus(Long id, WorkshopStatus status);

}
