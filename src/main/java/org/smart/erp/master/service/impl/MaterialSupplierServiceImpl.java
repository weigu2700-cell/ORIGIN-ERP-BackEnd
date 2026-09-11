package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierAddDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierPageDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierUpdateDto;
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
import cn.idev.excel.FastExcel;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.master.vo.ExcelPrintVo.MaterialSupplierExportVo;
import org.smart.erp.master.vo.MaterialSupplierVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
    public void addMaterialSupplier(MaterialSupplierAddDto dto) {

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
                new LambdaQueryWrapper<MaterialSupplier>()
                        .eq(MaterialSupplier::getMaterialId, dto.getMaterialId())
                        .eq(MaterialSupplier::getSupplierId, dto.getSupplierId())
                        .eq(MaterialSupplier::getMaterialSupplierCode, dto.getMaterialSupplierCode())
        );

        if (materialSupplier != null) {
            throw new BusinessException(404,"物料供应商关系已存在");
        }

        MaterialSupplier entity = new MaterialSupplier();
        BeanUtils.copyProperties(dto, entity);
        // 新建关联默认有效，避免状态字段为空后被前端显示为无效
        entity.setStatus(MaterialSupplierStatus.ACTIVE);
        materialSupplierMapper.insert(entity);
    }

    @Override
    public Page<MaterialSupplierVo> pageMaterialSupplier(MaterialSupplierPageDto dto) {

        LambdaQueryWrapper<MaterialSupplier> queryWrapper =
                new LambdaQueryWrapper<MaterialSupplier>()
                        .eq(dto.getMaterialId() != null, MaterialSupplier::getMaterialId, dto.getMaterialId())
                        .eq(dto.getSupplierId() != null, MaterialSupplier::getSupplierId, dto.getSupplierId())
                        .like(dto.getMaterialSupplierCode() != null, MaterialSupplier::getMaterialSupplierCode, dto.getMaterialSupplierCode())
                        .eq(dto.getStatus() != null, MaterialSupplier::getStatus, dto.getStatus())
                        .eq(dto.getPreferred() != null, MaterialSupplier::getPreferred, dto.getPreferred());

        Page<MaterialSupplier> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        List<Long> materialIds = page.getRecords().stream().map(MaterialSupplier::getMaterialId).distinct().toList();
        List<Long> supplierIds = page.getRecords().stream().map(MaterialSupplier::getSupplierId).distinct().toList();

        Map<Long, String> materialNameMap = materialMapper
                        .selectByIds(materialIds)
                        .stream()
                        .collect(Collectors.toMap(Material::getId, Material::getName));

        Map<Long, String> supplierNameMap = supplierMapper
                        .selectByIds(supplierIds)
                        .stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        return PageConvertUtils.convert(page, item -> {
            MaterialSupplierVo voPage = new MaterialSupplierVo();
            BeanUtils.copyProperties(item, voPage);
            voPage.setMaterialName(materialNameMap.get(item.getMaterialId()));
            voPage.setSupplierName(supplierNameMap.get(item.getSupplierId()));
            if (item.getStatus() != null) {
                voPage.setStatus(item.getStatus().getCode());
            }
            return voPage;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialSupplierVo getMaterialSupplier(Long id) {

        MaterialSupplier materialSupplier = materialSupplierMapper.selectById(id);
        if (materialSupplier == null || materialSupplier.getStatus() == MaterialSupplierStatus.INACTIVE) {
            throw new BusinessException(404,"物料供应商关系不存在或被禁用");
        }

        String materialName = materialMapper.selectById(materialSupplier.getMaterialId()).getName();
        String supplierName = supplierMapper.selectById(materialSupplier.getSupplierId()).getName();

        MaterialSupplierVo vo = new MaterialSupplierVo();
        BeanUtils.copyProperties(materialSupplier, vo);
        vo.setMaterialName(materialName);
        vo.setSupplierName(supplierName);
        if (materialSupplier.getStatus() != null) {
            vo.setStatus(materialSupplier.getStatus().getCode());
        }
        return vo;
    }

    @Override
    public void updateMaterialSupplier(Long id, MaterialSupplierUpdateDto dto) {
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

    @Override
    public void exportMaterialSupplier(List<Long> ids, HttpServletResponse response) {
        LambdaQueryWrapper<MaterialSupplier> queryWrapper = new LambdaQueryWrapper<>();
        Optional.ofNullable(ids).ifPresent(idList -> queryWrapper.in(MaterialSupplier::getId, idList));

        List<MaterialSupplier> materialSuppliers = this.list(queryWrapper);

        List<Long> materialIds = materialSuppliers.stream().map(MaterialSupplier::getMaterialId).distinct().toList();
        List<Long> supplierIds = materialSuppliers.stream().map(MaterialSupplier::getSupplierId).distinct().toList();

        Map<Long, String> materialNameMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectByIds(materialIds).stream()
                        .collect(Collectors.toMap(Material::getId, Material::getName));
        Map<Long, String> supplierNameMap = supplierIds.isEmpty() ? Map.of()
                : supplierMapper.selectByIds(supplierIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        List<MaterialSupplierExportVo> data = materialSuppliers.stream()
                .map(item -> {
                    MaterialSupplierExportVo vo = new MaterialSupplierExportVo();
                    BeanUtils.copyProperties(item, vo);
                    vo.setMaterialName(materialNameMap.get(item.getMaterialId()));
                    vo.setSupplierName(supplierNameMap.get(item.getSupplierId()));
                    if (item.getStatus() != null) {
                        vo.setStatusDesc(item.getStatus().getDesc());
                    }
                    vo.setPreferredDesc(item.getPreferred() != null && item.getPreferred() == 1 ? "是" : "否");
                    return vo;
                })
                .toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        try {
            String fileName = URLEncoder
                    .encode("物料供应商", StandardCharsets.UTF_8)
                    .replace("+", "%20");

            response.setHeader(
                    "Content-Disposition",
                    "attachment;filename*=utf-8''" + fileName + ".xlsx"
            );

            FastExcel.write(response.getOutputStream(), MaterialSupplierExportVo.class)
                    .sheet("物料供应商")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("导出物料供应商失败", e);
            throw new BusinessException(500, "导出物料供应商失败");
        }
    }

}
