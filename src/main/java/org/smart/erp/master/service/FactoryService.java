package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.FactoryDto.FactoryAddDto;
import org.smart.erp.master.dto.FactoryDto.FactoryPageDto;
import org.smart.erp.master.dto.FactoryDto.FactoryUpdateDto;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.vo.FactoryVo;

import java.util.List;

public interface FactoryService extends IService<Factory> {
    void addFactory(FactoryAddDto dto);

    Page<FactoryVo> getFactoryList(FactoryPageDto dto);

    void updateFactory(Long id , FactoryUpdateDto dto);

    FactoryVo detailFactory(Long id);

    void updateFactoryStatus(Long id, FactoryStatus status);

    void exportFactory(List<Long> ids, HttpServletResponse response);
}
