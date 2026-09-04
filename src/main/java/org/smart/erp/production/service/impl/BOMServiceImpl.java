package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.enums.BOMStatus;
import org.smart.erp.production.mapper.BOMItemMapper;
import org.smart.erp.production.mapper.BOMMapper;
import org.smart.erp.production.service.BOMItemService;
import org.smart.erp.production.service.BOMService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class BOMServiceImpl
        extends ServiceImpl<BOMMapper, BOM>
        implements BOMService
{

    private final MaterialMapper materialMapper;
    private final BOMItemMapper bomItemMapper;
    private final BOMItemService bomItemService;
    private final BusinessNoGenerator businessNoGenerator;

    public BOMServiceImpl(
            MaterialMapper materialMapper,
            BOMItemMapper bomItemMapper,
            BOMItemService bomItemService,
            BusinessNoGenerator businessNoGenerator
    )
    {
        this.materialMapper = materialMapper;
        this.bomItemMapper = bomItemMapper;
        this.bomItemService = bomItemService;
        this.businessNoGenerator = businessNoGenerator;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBOM(creatBOMDto dto) {

        Material material = materialMapper.selectById(dto.getMaterialId());

        if (Objects.isNull(material) || material.getStatus().equals(MaterialStatus.DISABLE)) {
            throw new BusinessException(404,"物料不存在或已被禁用");
        }
        if (dto.getBomItems() == null || dto.getBomItems().isEmpty()) {
            throw new BusinessException(400, "BOM 明细不能为空");
        }

        Set<Long> componentMaterialIds = new HashSet<>();
        for (createBOMItemDto bomItemDto : dto.getBomItems()) {
            if (!componentMaterialIds.add(bomItemDto.getComponentMaterialId())) {
                throw new BusinessException(400, "BOM中存在重复组成物料");
            }
        }

        BOM latestBom = baseMapper.selectOne(
                new LambdaQueryWrapper<BOM>()
                        .eq(BOM::getMaterialId, dto.getMaterialId())
                        .orderByDesc(BOM::getVersion)
                        .last("limit 1")
        );
        int version = latestBom == null ? 1 : latestBom.getVersion() + 1;

        BOM bom = new BOM();
        bom.setBomNo(businessNoGenerator.generateNo("erp:sequence:bom:", "BOM"));
        bom.setMaterialId(dto.getMaterialId());
        bom.setStatus(BOMStatus.DRAFT);
        bom.setVersion(version);
        baseMapper.insert(bom);

        int lineNo = 10;
        for (createBOMItemDto bomItemDto : dto.getBomItems()) {
            bomItemDto.setBomId(bom.getId());
            bomItemService.createBOMItem(bomItemDto, lineNo);
            lineNo += 10;
        }
    }
}
