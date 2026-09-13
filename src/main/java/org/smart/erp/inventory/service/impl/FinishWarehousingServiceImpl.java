package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.inventory.entity.FinishWarehousing;
import org.smart.erp.inventory.mapper.FinishWarehousingMapper;
import org.smart.erp.inventory.service.FinishWarehousingService;
import org.springframework.stereotype.Service;

@Service
public class FinishWarehousingServiceImpl
    extends ServiceImpl<FinishWarehousingMapper, FinishWarehousing>
        implements FinishWarehousingService
{
}
