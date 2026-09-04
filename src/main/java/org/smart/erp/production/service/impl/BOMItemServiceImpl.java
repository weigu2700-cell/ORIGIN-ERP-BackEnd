package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private Map<Long, Material> getMaterialList(List<Long> materialIds) {

        List<Material> materials = materialMapper.selectByIds(materialIds);
        return materials.stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
    }


    @Override
    public BOMItemVo createBOMItem(createBOMItemDto dto,Long bomId, Integer lineNo) {
        BOM bom = bomMapper.selectById(bomId);
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }
        Material material = materialMapper.selectById(dto.getComponentMaterialId());
        if (Objects.isNull(material) || material.getStatus() == MaterialStatus.DISABLE) {
            throw new BusinessException(404,"物料不存在或已被禁用");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "物料数量必须大于0");
        }
        if (dto.getComponentMaterialId().equals(bom.getMaterialId())) {
            throw new BusinessException(400,"物料不能是BOM物料");
        }
        BigDecimal lossRate = dto.getLossRate() == null ? BigDecimal.ZERO : dto.getLossRate();
        if (lossRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "损耗率不能小于0");
        }

        BOMItem bomItem = new BOMItem();
        BeanUtils.copyProperties(dto,bomItem);
        bomItem.setLineNo(lineNo);
        bomItem.setLossRate(lossRate);
        save(bomItem);

        BOMItemVo vo = new BOMItemVo();
        BeanUtils.copyProperties(bomItem,vo);
        return vo;
    }

    @Override
    public List<BOMItemVo> getBOMItemList(Long bomId) {
        List<BOMItem> bomItems = list(
                new LambdaQueryWrapper<BOMItem>().eq(BOMItem::getBomId, bomId)
        );

        List<Long> componentMaterialIds = bomItems.stream()
                .map(BOMItem::getComponentMaterialId).distinct().toList();

        Map<Long,Material> materials = getMaterialList(componentMaterialIds);

        List<BOMItemVo> bomItemVos = new ArrayList<>();
        for (BOMItem bomItem : bomItems) {
            BOMItemVo vo = new BOMItemVo();
            BeanUtils.copyProperties(bomItem,vo);
            Material material = materials.get(bomItem.getComponentMaterialId());
            vo.setComponentMaterialCode(material.getCode());
            vo.setComponentMaterialName(material.getName());
            bomItemVos.add(vo);
        }
        return bomItemVos;
    }

    @Override
    public Map<Long, List<BOMItemVo>> getBOMItemMap(List<Long> bomIds) {
        List<BOMItem> bomItems = list(
                new LambdaQueryWrapper<BOMItem>().in(BOMItem::getBomId, bomIds)
        );

        List<Long> componentMaterialIds = bomItems.stream()
                .map(BOMItem::getComponentMaterialId).distinct().toList();

        Map<Long,Material> materials = getMaterialList(componentMaterialIds);

        Map<Long,List<BOMItemVo>> bomItemMap = new HashMap<>();
        for (BOMItem bomItem : bomItems) {
            BOMItemVo vo = new BOMItemVo();
            BeanUtils.copyProperties(bomItem,vo);
            Material material = materials.get(bomItem.getComponentMaterialId());
            vo.setComponentMaterialCode(material.getCode());
            vo.setComponentMaterialName(material.getName());
            bomItemMap.computeIfAbsent(bomItem.getBomId(),k->new ArrayList<>()).add(vo);
        }
        return bomItemMap;

    }
}
