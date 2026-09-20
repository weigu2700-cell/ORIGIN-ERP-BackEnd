package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.master.dto.WarehouseDto.WarehouseAddDto;
import org.smart.erp.master.dto.WarehouseDto.WarehousePageDto;
import org.smart.erp.master.dto.WarehouseDto.WarehouseUpdateDto;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.vo.WarehouseVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WarehouseService extends IService<Warehouse> {
    @PreAuthorize("hasAnyAuthority('master:warehouse:create')")
    void add(WarehouseAddDto dto);

    @PreAuthorize("hasAnyAuthority('master:warehouse:list')")
    Page<WarehouseVo> getWarehouseList(WarehousePageDto dto);

    @PreAuthorize("hasAnyAuthority('master:warehouse:get')")
    WarehouseVo getWarehouse(Long id);

    @PreAuthorize("hasAnyAuthority('master:warehouse:update')")
    void updateWarehouse(Long id, WarehouseUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('master:warehouse:status')")
    void updateWarehouseStatus(Long id, WarehouseStatus status);
}
