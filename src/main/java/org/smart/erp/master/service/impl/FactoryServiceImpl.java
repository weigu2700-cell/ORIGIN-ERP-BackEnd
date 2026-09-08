package org.smart.erp.master.service.impl;

import cn.idev.excel.FastExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.SnowflakeIdGenerator;
import org.smart.erp.master.dto.FactoryDTO.FactoryCreateDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryListDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryUpdateDTO;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.mapper.FactoryMapper;
import org.smart.erp.master.mapper.WorkshopMapper;
import org.smart.erp.master.service.FactoryService;
import org.smart.erp.master.vo.FactoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class FactoryServiceImpl extends ServiceImpl<FactoryMapper, Factory> implements FactoryService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private final WorkshopMapper workshopMapper;

    public FactoryServiceImpl(WorkshopMapper workshopMapper) {
        this.workshopMapper = workshopMapper;
    }

    @Override
    public void createFactory(FactoryCreateDTO dto) {
        Factory factory = new Factory();

        LambdaQueryWrapper<Factory> existsWrapper = new LambdaQueryWrapper<Factory>()
                .eq(Factory::getName, dto.getName());
        if (this.count(existsWrapper) > 0) {
            throw new BusinessException(400, "工厂名称已存在");
        }
        BeanUtils.copyProperties(dto, factory);
        factory.setCode("FT" + SNOWFLAKE.nextId());
        factory.setStatus(FactoryStatus.ENABLE);
        save(factory);
    }

    @Override
    public Page<FactoryVO> getFactoryList(FactoryListDTO dto) {
        LambdaQueryWrapper<Factory> queryWrapper = new LambdaQueryWrapper<Factory>()
                .eq(StringUtils.hasText(dto.getCode()), Factory::getCode, dto.getCode())
                .like(StringUtils.hasText(dto.getName()), Factory::getName, dto.getName())
                .like(StringUtils.hasText(dto.getShortName()), Factory::getShortName, dto.getShortName());

        Page<Factory> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()) , queryWrapper);
        return PageConvertUtils.convert(page, this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFactory(Long id, FactoryUpdateDTO dto) {
        Factory factory = this.getById(id);
        if (factory == null) {
            throw new BusinessException(400, "工厂不存在");
        }

        if (StringUtils.hasText(dto.getName())) factory.setName(dto.getName());
        if (StringUtils.hasText(dto.getShortName())) factory.setShortName(dto.getShortName());
        if (StringUtils.hasText(dto.getAddress())) factory.setAddress(dto.getAddress());
        if (StringUtils.hasText(dto.getRemark())) factory.setRemark(dto.getRemark());

        updateById(factory);
    }

    @Override
    public FactoryVO getFactoryById(Long id) {
        Factory factory = this.getById(id);
        if (factory == null) {
            throw new BusinessException(400, "工厂不存在");
        }
        return toVO(factory);
    }

    private FactoryVO toVO(Factory factory) {
        FactoryVO vo = new FactoryVO();
        BeanUtils.copyProperties(factory, vo);
        vo.setId(String.valueOf(factory.getId()));
        if (factory.getStatus() != null) {
            vo.setStatus(factory.getStatus().getCode());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFactoryStatus(Long id, FactoryStatus status) {
        Factory factory = this.getById(id);
        if (factory == null) {
            throw new BusinessException(400, "工厂不存在");
        }
        factory.setStatus(status);

        if (status == FactoryStatus.DISABLE) {
            Workshop workshop = new Workshop();
            workshop.setStatus(WorkshopStatus.DISABLE);
            workshopMapper.update(workshop, new LambdaQueryWrapper<Workshop>()
                    .eq(Workshop::getFactoryId, factory.getId()));
        }
        updateById(factory);
    }

    @Override
    public void exportFactory(List<Long> ids, HttpServletResponse response) {
        LambdaQueryWrapper<Factory> queryWrapper = new LambdaQueryWrapper<>();

        if (ids != null && !ids.isEmpty()) {
            queryWrapper.in(Factory::getId, ids);
        }
        List<Factory> factories = this.list(queryWrapper);
        List<FactoryVO> data = factories.stream().map(this::toVO).toList();

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("UTF-8");

        try {
            String filed = URLEncoder
                    .encode("工厂信息", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + filed + ".xlsx");
            FastExcel.write(response.getOutputStream(), FactoryVO.class).sheet().doWrite(data);
        }
        catch (IOException e) {
            log.error("导出工厂信息失败", e);
            throw new BusinessException(500, "导出失败");
        }

    }
}
