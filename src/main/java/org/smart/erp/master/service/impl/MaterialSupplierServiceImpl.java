package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierCreateDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierListDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierUpdateDTO;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.MaterialSupplier;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.MaterialSupplierStatus;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.MaterialSupplierMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.service.MaterialSupplierService;
import org.smart.erp.master.vo.MaterialSupplierVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialSupplierServiceImpl
        extends ServiceImpl<MaterialSupplierMapper, MaterialSupplier>
        implements MaterialSupplierService
{
    private final MaterialSupplierMapper materialSupplierMapper;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;

    public MaterialSupplierServiceImpl(
            MaterialSupplierMapper materialSupplierMapper,
            MaterialMapper materialMapper,
            SupplierMapper supplierMapper
    )
    {
        this.materialSupplierMapper = materialSupplierMapper;
        this.materialMapper = materialMapper;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public void createMaterialSupplier(MaterialSupplierCreateDTO dto) {

        Material material = materialMapper.selectById(dto.getMaterialId());
        if (material == null) {
            throw new BusinessException(404,"物料不存在");
        }
        if (material.getStatus() != MaterialStatus.ENABLE) {
            throw new BusinessException(400,"物料已禁用");
        }

        Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null) {
            throw new BusinessException(404,"供应商不存在");
        }
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BusinessException(400,"供应商已禁用");
        }

        MaterialSupplier materialSupplier = materialSupplierMapper.selectOne(
                new QueryWrapper<MaterialSupplier>()
                        .eq("material_id", dto.getMaterialId())
                        .eq("supplier_id", dto.getSupplierId())
                        .eq("materialSupplierCode" , dto.getMaterialSupplierCode())
        );

        if (materialSupplier != null) {
            throw new BusinessException(404,"物料供应商关系已存在");
        }

        MaterialSupplier entity = new MaterialSupplier();
        BeanUtils.copyProperties(dto, entity);
        materialSupplierMapper.insert(entity);
    }

    @Override
    public Page<MaterialSupplierVO> listMaterialSupplier(MaterialSupplierListDTO dto) {

        LambdaQueryWrapper<MaterialSupplier> queryWrapper =
                new LambdaQueryWrapper<MaterialSupplier>()
                        .eq(dto.getMaterialId() != null, MaterialSupplier::getMaterialId, dto.getMaterialId())
                        .eq(dto.getSupplierId() != null, MaterialSupplier::getSupplierId, dto.getSupplierId())
                        .like(dto.getMaterialSupplierCode() != null, MaterialSupplier::getMaterialSupplierCode, dto.getMaterialSupplierCode())
                        .eq(dto.getStatus() != null, MaterialSupplier::getStatus, dto.getStatus())
                        .eq(dto.getPreferred() != null, MaterialSupplier::getPreferred, dto.getPreferred());

        Page<MaterialSupplier> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        List<Long> materialSupplierIds = page.getRecords().stream().map(MaterialSupplier::getMaterialId).toList();

        Map<Long, String> materialNameMap = materialMapper
                        .selectByIds(materialSupplierIds)
                        .stream()
                        .collect(Collectors.toMap(Material::getId, Material::getName));

        Map<Long, String> supplierNameMap = supplierMapper
                        .selectByIds(materialSupplierIds)
                        .stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        return PageConvertUtils.convert(page, item -> {
            MaterialSupplierVO voPage = new MaterialSupplierVO();
            BeanUtils.copyProperties(item, voPage);
            voPage.setMaterialName(materialNameMap.get(item.getMaterialId()));
            voPage.setSupplierName(supplierNameMap.get(item.getSupplierId()));
            return voPage;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialSupplierVO getMaterialSupplier(Long id) {

        MaterialSupplier materialSupplier = materialSupplierMapper.selectById(id);
        if (materialSupplier == null || materialSupplier.getStatus() == MaterialSupplierStatus.INACTIVE) {
            throw new BusinessException(404,"物料供应商关系不存在或被禁用");
        }

        String materialName = materialMapper.selectById(materialSupplier.getMaterialId()).getName();
        String supplierName = supplierMapper.selectById(materialSupplier.getSupplierId()).getName();

        MaterialSupplierVO vo = new MaterialSupplierVO();
        BeanUtils.copyProperties(materialSupplier, vo);
        vo.setMaterialName(materialName);
        vo.setSupplierName(supplierName);
        return vo;
    }

    @Override
    public void updateMaterialSupplier(Long id, MaterialSupplierUpdateDTO dto) {
        MaterialSupplier materialSupplier = materialSupplierMapper.selectById(id);
        if (materialSupplier == null || materialSupplier.getStatus() == MaterialSupplierStatus.INACTIVE) {
            throw new BusinessException(404,"物料供应商关系不存在或被禁用");
        }

        if (dto.getLeadTimeDays() != null) materialSupplier.setLeadTimeDays(dto.getLeadTimeDays());
        if (dto.getMaterialSupplierCode() != null) materialSupplier.setMaterialSupplierCode(dto.getMaterialSupplierCode());
        if (dto.getMinOrderQty() != null) materialSupplier.setMinOrderQty(dto.getMinOrderQty());
        if (dto.getPurchasePrice() != null) materialSupplier.setPurchasePrice(dto.getPurchasePrice());
        if (dto.getPreferred() != null) materialSupplier.setPreferred(dto.getPreferred());
        if (dto.getRemark() != null) materialSupplier.setRemark(dto.getRemark());

        materialSupplierMapper.updateById(materialSupplier);
    }

    @Override
    public void changeMaterialSupplierStatus(Long id) {
        MaterialSupplier materialSupplier = materialSupplierMapper.selectById(id);
        if (materialSupplier == null) {
            throw new BusinessException(404,"物料供应商关系不存在");
        }
        materialSupplier.setStatus(materialSupplier.getStatus() == MaterialSupplierStatus.ACTIVE
                ? MaterialSupplierStatus.INACTIVE
                : MaterialSupplierStatus.ACTIVE);
        materialSupplierMapper.updateById(materialSupplier);
    }

    @Override
    public void changeMaterialSupplierPreferred(Long materialId , Long supplierId) {

        MaterialSupplier materialSupplier = materialSupplierMapper.selectOne(
                new LambdaQueryWrapper<MaterialSupplier>()
                        .eq(MaterialSupplier::getMaterialId, materialId)
                        .eq(MaterialSupplier::getSupplierId, supplierId)
        );
        if (materialSupplier == null) {
            throw new BusinessException(404,"物料供应商关系不存在");
        }
        materialSupplier.setPreferred(materialSupplier.getPreferred() == 1 ? 0 : 1);
        MaterialSupplier preferredMaterialSupplier =
                materialSupplierMapper.selectOne
                        (
                            new LambdaQueryWrapper<MaterialSupplier>()
                                    .eq(MaterialSupplier::getPreferred, 1)
                        );
        if (preferredMaterialSupplier != null) {
            preferredMaterialSupplier.setPreferred(0);
            materialSupplierMapper.updateById(preferredMaterialSupplier);
        }
        materialSupplierMapper.updateById(materialSupplier);
    }

}
