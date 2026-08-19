package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseCreateDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseListDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseUpdateDTO;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.vo.WarehouseVO;

public interface WarehouseService extends IService<Warehouse> {
    void create(WarehouseCreateDTO dto);

    Page<WarehouseVO> getWarehouseList(WarehouseListDTO dto);

    WarehouseVO getWarehouse(Long id);

    void updateWarehouse(Long id, WarehouseUpdateDTO dto);

    void updateWarehouseStatus(Long id, WarehouseStatus status);
}
