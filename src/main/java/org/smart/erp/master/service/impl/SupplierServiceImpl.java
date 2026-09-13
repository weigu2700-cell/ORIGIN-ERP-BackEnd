package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.SnowflakeIdGenerator;
import org.smart.erp.master.convertor.ApplyUpdate;
import org.smart.erp.master.dto.SupplierDto.SupplierAddDto;
import org.smart.erp.master.dto.SupplierDto.SupplierPageDto;
import org.smart.erp.master.dto.SupplierDto.SupplierUpdateDto;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.service.SupplierService;
import org.smart.erp.master.vo.SupplierVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.idev.excel.FastExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.master.vo.SupplierExportVo;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private SupplierVo convertToVO(Supplier supplier) {
        SupplierVo vo = new SupplierVo();
        BeanUtils.copyProperties(supplier, vo);
        if (supplier.getStatus() != null) {
            vo.setStatus(supplier.getStatus().getCode());
        }
        vo.setCreatedTime(supplier.getCreateTime());
        return vo;
    }

    @Override
    public void addSupplier(SupplierAddDto dto) {
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        supplier.setCode("SU"  + SNOWFLAKE.nextId());
        supplier.setStatus(SupplierStatus.ACTIVE);
        this.save(supplier);
    }

    @Override
    public Page<SupplierVo> pageSupplier(SupplierPageDto dto) {
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
    public SupplierVo detailSupplier(Long id) {
        Supplier supplier = this.getById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        return convertToVO(supplier);
    }

    @Override
    public void updateSupplier(Long id, SupplierUpdateDto dto) {
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

    @Override
    public void exportSupplier(List<Long> ids, HttpServletResponse response) {
        LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            queryWrapper.in(Supplier::getId, ids);
        }
        List<Supplier> suppliers = this.list(queryWrapper);

        List<SupplierExportVo> data = suppliers.stream()
                .map(supplier -> {
                    SupplierExportVo vo = new SupplierExportVo();
                    BeanUtils.copyProperties(supplier, vo);
                    if (supplier.getStatus() != null) {
                        vo.setStatusDesc(supplier.getStatus().getDesc());
                    }
                    return vo;
                })
                .toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            String fileName = URLEncoder.encode("供应商", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            FastExcel.write(response.getOutputStream(), SupplierExportVo.class)
                    .sheet("供应商")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("导出供应商失败", e);
            throw new BusinessException(500, "导出供应商失败");
        }
    }
}
