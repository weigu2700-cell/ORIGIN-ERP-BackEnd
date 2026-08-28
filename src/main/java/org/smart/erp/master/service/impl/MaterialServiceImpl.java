package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.SnowflakeIdGenerator;
import org.smart.erp.master.dto.MaterialDTO.MaterialCreateDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialListDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialUpdateDTO;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.service.MaterialService;
import org.smart.erp.master.vo.MaterialVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    private final MaterialMapper materialMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);

    public MaterialServiceImpl(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    private MaterialVO convertToVO(Material material) {
        MaterialVO vo = new MaterialVO();
        BeanUtils.copyProperties(material, vo);
        return vo;
    }

    @Override
    public void createMaterial(MaterialCreateDTO dto) {
        Material material = new Material();
        BeanUtils.copyProperties(dto, material);

        material.setCode("MM" + snowflakeIdGenerator.nextId());
        material.setStatus(MaterialStatus.ENABLE);
        materialMapper.insert(material);
    }

    @Override
    public Page<MaterialVO> listMaterial(MaterialListDTO dto) {
        LambdaQueryWrapper<Material> queryWrapper =
                new LambdaQueryWrapper<Material>()
                        .like(dto.getCode() != null, Material::getCode, dto.getCode())
                        .like(dto.getName() != null, Material::getName, dto.getName())
                        .eq(dto.getType() != null, Material::getType, dto.getType())
                        .eq(dto.getSpec() != null, Material::getSpec, dto.getSpec())
                        .eq(dto.getStatus() != null, Material::getStatus, dto.getStatus());

       Page<Material> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
       return PageConvertUtils.convert(page, MaterialVO.class);
    }

    @Override
    public MaterialVO getMaterialDetail(Long id) {
        Material material = materialMapper.selectById(id);
        return convertToVO(material);
    }

    @Override
    public void updateMaterial(Long id, MaterialUpdateDTO dto) {
        Material material = materialMapper.selectById(id);

        if (material == null) {
            throw new BusinessException(404, "物料不存在");
        }

        if (dto.getName() != null) material.setName(dto.getName());
        if (dto.getSpec() != null) material.setSpec(dto.getSpec());
        if (dto.getUnit() != null) material.setUnit(dto.getUnit());
        if (dto.getSafetyStock() != null) material.setSafetyStock(dto.getSafetyStock());
        if (dto.getRemark() != null) material.setRemark(dto.getRemark());

        materialMapper.updateById(material);
    }

    @Override
    public void changeMaterialStatus(Long id, MaterialStatus status) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(404, "物料不存在");
        }
        material.setStatus(status);
        materialMapper.updateById(material);
    }


}
