package org.smart.erp.production.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.entity.BOMItem;
import org.smart.erp.production.vo.BOMItemVo;

import java.util.List;
import java.util.Map;

public interface BOMItemService extends IService<BOMItem> {

    BOMItemVo createBOMItem(createBOMItemDto dto,Long bomId, Integer lineNO);

    List<BOMItemVo> getBOMItemList(Long bomId);

    Map<Long,List<BOMItemVo>> getBOMItemMap(List<Long> bomIds);
}
