package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.WarehouseDto.WarehouseAddDto;
import org.smart.erp.master.dto.WarehouseDto.WarehousePageDto;
import org.smart.erp.master.dto.WarehouseDto.WarehouseUpdateDto;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.vo.WarehouseVo;

public interface WarehouseService extends IService<Warehouse> {
    void add(WarehouseAddDto dto);

    Page<WarehouseVo> getWarehouseList(WarehousePageDto dto);

    WarehouseVo getWarehouse(Long id);

    void updateWarehouse(Long id, WarehouseUpdateDto dto);

    void updateWarehouseStatus(Long id, WarehouseStatus status);
}
