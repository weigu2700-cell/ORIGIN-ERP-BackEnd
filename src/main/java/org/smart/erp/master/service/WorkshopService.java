package org.smart.erp.master.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopCreateDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopListDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopUpdateDTO;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.vo.WorkshopVO;

public interface WorkshopService extends IService<Workshop> {
    void createWorkshop(WorkshopCreateDTO dto);

    Page<WorkshopVO> listWorkshop(WorkshopListDTO dto);

    WorkshopVO getWorkshopDetail(Long id);

    void updateWorkshop(Long id, WorkshopUpdateDTO dto);

    void changeStatus(Long id, WorkshopStatus status);

}
