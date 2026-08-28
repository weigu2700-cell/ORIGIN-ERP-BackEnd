package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.SnowflakeIdGenerator;
import org.smart.erp.master.convertor.ApplyUpdate;
import org.smart.erp.master.dto.SupplierDTO.SupplierCreateDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierListDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierUpdateDTO;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.service.SupplierService;
import org.smart.erp.master.vo.SupplierVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private SupplierVO convertToVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        if (supplier.getStatus() != null) {
            vo.setStatus(supplier.getStatus().getCode());
        }
        vo.setCreatedTime(supplier.getCreateTime());
        return vo;
    }

    @Override
    public void createSupplier(SupplierCreateDTO dto) {
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        supplier.setCode("SU"  + SNOWFLAKE.nextId());
        supplier.setStatus(SupplierStatus.ACTIVE);
        this.save(supplier);
    }

    @Override
    public Page<SupplierVO> listSupplier(SupplierListDTO dto) {
        LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<Supplier>()
                .eq(StringUtils.hasText(dto.getCode()), Supplier::getCode, dto.getCode())
                .like(StringUtils.hasText(dto.getName()), Supplier::getName, dto.getName())
                .like(StringUtils.hasText(dto.getShortName()), Supplier::getShortName, dto.getShortName())
                .like(StringUtils.hasText(dto.getContactName()), Supplier::getContactName, dto.getContactName())
                .like(StringUtils.hasText(dto.getPhone()), Supplier::getPhone, dto.getPhone())
                .like(StringUtils.hasText(dto.getEmail()), Supplier::getEmail, dto.getEmail())
                .orderByDesc(Supplier::getCreateTime);

        Page<Supplier> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        return PageConvertUtils.convert(page, this::convertToVO);
    }

    @Override
    public SupplierVO getSupplierDetail(Long id) {
        Supplier supplier = this.getById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        return convertToVO(supplier);
    }

    @Override
    public void updateSupplier(Long id, SupplierUpdateDTO dto) {
        Supplier supplier = this.getById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        ApplyUpdate.setUpdateValue(supplier, dto);
        this.updateById(supplier);
    }

    @Override
    public void changeSupplierStatus(Long id, SupplierStatus status) {
        Supplier supplier = this.getById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        supplier.setStatus(status);
        this.updateById(supplier);
    }
}
