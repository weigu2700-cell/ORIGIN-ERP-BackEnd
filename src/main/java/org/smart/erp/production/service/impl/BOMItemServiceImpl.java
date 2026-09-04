package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.entity.BOMItem;
import org.smart.erp.production.mapper.BOMItemMapper;
import org.smart.erp.production.mapper.BOMMapper;
import org.smart.erp.production.service.BOMItemService;
import org.smart.erp.production.vo.BOMItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class BOMItemServiceImpl
    extends ServiceImpl<BOMItemMapper, BOMItem>
    implements BOMItemService
{

    private final BOMMapper bomMapper;
    private final MaterialMapper materialMapper;

    public BOMItemServiceImpl(
            BOMMapper bomMapper,
            MaterialMapper materialMapper
    )
    {
        this.bomMapper = bomMapper;
        this.materialMapper = materialMapper;
    }


    @Override
    public BOMItemVo createBOMItem(createBOMItemDto dto, Integer lineNo) {
        BOM bom = bomMapper.selectById(dto.getBomId());
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }
        Material material = materialMapper.selectById(dto.getComponentMaterialId());
        if (Objects.isNull(material)) {
            throw new BusinessException(404,"物料不存在");
        }
        if (dto.getComponentMaterialId().equals(bom.getMaterialId())) {
            throw new BusinessException(400,"物料不能是BOM物料");
        }
        if (dto.getLossRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "损耗率不能小于0");
        }

        BOMItem bomItem = new BOMItem();
        BeanUtils.copyProperties(dto,bomItem);
        bomItem.setLineNo(lineNo);
        save(bomItem);

        BOMItemVo vo = new BOMItemVo();
        BeanUtils.copyProperties(bomItem,vo);
        return vo;
    }
}
