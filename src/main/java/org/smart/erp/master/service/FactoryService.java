package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.FactoryDTO.FactoryCreateDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryListDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryUpdateDTO;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.vo.FactoryVO;

public interface FactoryService extends IService<Factory> {
    void createFactory(FactoryCreateDTO dto);

    Page<FactoryVO> getFactoryList(FactoryListDTO dto);

    void updateFactory(Long id , FactoryUpdateDTO dto);

    FactoryVO getFactoryById(Long id);

    void updateFactoryStatus(Long id, FactoryStatus status);
}
