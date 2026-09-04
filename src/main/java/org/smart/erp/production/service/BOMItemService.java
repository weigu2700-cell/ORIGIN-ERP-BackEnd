package org.smart.erp.production.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.entity.BOMItem;
import org.smart.erp.production.vo.BOMItemVo;

public interface BOMItemService extends IService<BOMItem> {

    BOMItemVo createBOMItem(createBOMItemDto dto, Integer lineNO);
}
