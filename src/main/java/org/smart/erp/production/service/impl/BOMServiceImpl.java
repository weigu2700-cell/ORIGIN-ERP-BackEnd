package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.dto.pageBOMDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.enums.BOMStatus;
import org.smart.erp.production.mapper.BOMItemMapper;
import org.smart.erp.production.mapper.BOMMapper;
import org.smart.erp.production.service.BOMItemService;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.vo.BOMItemVo;
import org.smart.erp.production.vo.BOMVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

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
            if (bomItemDto.getComponentMaterialId() == null) {
                throw new BusinessException(400, "组成物料不能为空");
            }
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
            bomItemService.createBOMItem(bomItemDto, bom.getId(),lineNo);
            lineNo += 10;
        }
    }

    @Override
    public BOMVo getBOMDetailById(Long id) {
        BOM bom = baseMapper.selectById(id);
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }

        Material material = materialMapper.selectById(bom.getMaterialId());

        BOMVo vo = new BOMVo();
        BeanUtils.copyProperties(bom,vo);
        vo.setMaterialCode(material.getCode());
        vo.setMaterialName(material.getName());
        vo.setBomItems(bomItemService.getBOMItemList(bom.getId()));
        return vo;
    }

    @Override
    public Page<BOMVo> getPageBOMVo(pageBOMDto dto) {
        LambdaQueryWrapper<BOM> queryWrapper =
                new LambdaQueryWrapper<BOM>()
                        .like(StringUtils.hasText(dto.getBomNo()),BOM::getBomNo,dto.getBomNo())
                        .eq(Objects.nonNull(dto.getMaterialId()),BOM::getMaterialId,dto.getMaterialId())
                        .eq(Objects.nonNull(dto.getStatus()),BOM::getStatus,dto.getStatus());

        Page<BOM> page = this.page(new Page<>(dto.getPageNum(),dto.getPageSize()),queryWrapper);

        List<Long> bomIds = page.getRecords().stream().map(BOM::getId).toList();
        Map<Long, List<BOMItemVo>> bomItemMap = bomItemService.getBOMItemMap(bomIds);

        Page<BOMVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(bom -> {
            BOMVo bomVo = new BOMVo();
            BeanUtils.copyProperties(bom, bomVo);
            bomVo.setBomItems(bomItemMap.get(bom.getId()));
            return bomVo;
        }).toList());

        return voPage;
    }
}
