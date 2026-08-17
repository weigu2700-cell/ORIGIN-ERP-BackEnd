package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.master.service.WarehouseService;
import org.springframework.stereotype.Service;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {
}
