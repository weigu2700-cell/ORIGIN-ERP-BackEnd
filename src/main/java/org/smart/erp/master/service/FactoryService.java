package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.FactoryDto.FactoryAddDto;
import org.smart.erp.master.dto.FactoryDto.FactoryPageDto;
import org.smart.erp.master.dto.FactoryDto.FactoryUpdateDto;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.vo.FactoryVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface FactoryService extends IService<Factory> {
    @PreAuthorize("hasAnyAuthority('factory:create')")
    void addFactory(FactoryAddDto dto);

    @PreAuthorize("hasAnyAuthority('factory:list')")
    Page<FactoryVo> getFactoryList(FactoryPageDto dto);

    @PreAuthorize("hasAnyAuthority('factory:update')")
    void updateFactory(Long id , FactoryUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('factory:get')")
    FactoryVo detailFactory(Long id);

    @PreAuthorize("hasAnyAuthority('factory:status:update')")
    void updateFactoryStatus(Long id, FactoryStatus status);

    @PreAuthorize("hasAnyAuthority('factory:export')")
    void exportFactory(List<Long> ids, HttpServletResponse response);
}
