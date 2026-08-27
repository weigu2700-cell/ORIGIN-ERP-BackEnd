package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.utils.DateCodeRuleUtil;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopCreateDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopListDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopUpdateDTO;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.mapper.ProductionLineMapper;
import org.smart.erp.master.mapper.WorkshopMapper;
import org.smart.erp.master.service.FactoryService;
import org.smart.erp.master.service.WorkshopService;
import org.smart.erp.master.vo.WorkshopVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkshopServiceImpl extends ServiceImpl<WorkshopMapper, Workshop> implements WorkshopService {

    private final WorkshopMapper workshopMapper;
    private final FactoryService factoryService;
    private final DateCodeRuleUtil dateCodeRuleUtil;
    private final ProductionLineMapper productionLineMapper;

    public WorkshopServiceImpl(
            WorkshopMapper workshopMapper ,
            DateCodeRuleUtil dateCodeRuleUtil,
            FactoryService factoryService,
            ProductionLineMapper productionLineMapper
    )
    {
        this.workshopMapper = workshopMapper;
        this.factoryService = factoryService;
        this.dateCodeRuleUtil = dateCodeRuleUtil;
        this.productionLineMapper = productionLineMapper;
    }

    @Override
    public void createWorkshop(WorkshopCreateDTO dto) {
        if (dto.getFactoryId() == null) {
            throw new BusinessException(400, "请选择所属工厂");
        }
        Factory factory = factoryService.getById(dto.getFactoryId());
        if (factory == null) {
            throw new BusinessException(404 , "工厂不存在");
        }
        if (factory.getStatus() == FactoryStatus.DISABLE) {
            throw new BusinessException(400 , "工厂已停用");
        }

        Workshop workshop = new Workshop();
        BeanUtils.copyProperties(dto , workshop);
        workshop.setCode(dateCodeRuleUtil.setDateCodeRule("WS" , workshop.getFactoryId()));
        workshop.setStatus(WorkshopStatus.ENABLE);
        workshopMapper.insert(workshop);
    }

    @Override
    public Page<WorkshopVO> listWorkshop(WorkshopListDTO dto) {
        LambdaQueryWrapper<Workshop> queryWrapper = new LambdaQueryWrapper<Workshop>()
                .like(StringUtils.hasText(dto.getCode()) , Workshop::getCode , dto.getCode())
                .like(StringUtils.hasText(dto.getName()) , Workshop::getName , dto.getName())
                .eq(dto.getFactoryId() != null , Workshop::getFactoryId , dto.getFactoryId());

        Page<Workshop> page = this.page(new Page<>(dto.getPage() , dto.getPageSize()) , queryWrapper);

        Map<Long , String> factoryNameMap = factoryService.list(new LambdaQueryWrapper<Factory>()
                .in(page.getRecords()
                                .stream()
                                .map(Workshop::getFactoryId)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .findAny()
                                .isPresent(), Factory::getId,
                        page.getRecords().
                                stream().
                                map(Workshop::getFactoryId).
                                filter(java.util.Objects::nonNull).
                                distinct()
                                .toList())
                .select(Factory::getId , Factory::getName))
                .stream()
                .collect(Collectors.toMap(Factory::getId , Factory::getName));

        Page<WorkshopVO> voPage = new Page<>();
        BeanUtils.copyProperties(page , voPage);
        voPage.setRecords(page.getRecords().stream()
                .map(workshop -> {
                    WorkshopVO vo = new WorkshopVO();
                    BeanUtils.copyProperties(workshop , vo);
                    vo.setFactoryName(factoryNameMap.get(workshop.getFactoryId()));
                    return vo;
                })
                .toList());
        return voPage;
    }

    @Override
    public WorkshopVO getWorkshopDetail(Long id) {
        Workshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(404 , "车间不存在");
        }
        Factory factory = factoryService.getById(workshop.getFactoryId());
        if (factory == null) {
            throw new BusinessException(404 , "工厂不存在");
        }
        if (factory.getStatus() == FactoryStatus.DISABLE) {
            workshop.setStatus(WorkshopStatus.DISABLE);
            workshopMapper.updateById(workshop);
        }
        WorkshopVO vo = new WorkshopVO();
        BeanUtils.copyProperties(workshop , vo);
        vo.setFactoryName(factory.getName());
        vo.setStatus(WorkshopStatus.DISABLE);
        return vo;
    }

    @Override
    public void updateWorkshop(Long id, WorkshopUpdateDTO dto) {
        Workshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(404 , "车间不存在");
        }

        if (StringUtils.hasText(dto.getName())) workshop.setName(dto.getName());
        if (StringUtils.hasText(dto.getShortName())) workshop.setShortName(dto.getShortName());
        if (StringUtils.hasText(dto.getRemark())) workshop.setRemark(dto.getRemark());
        if (dto.getFactoryId() != null)  workshop.setFactoryId(dto.getFactoryId());

        workshopMapper.updateById(workshop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, WorkshopStatus status) {
        if (status == null) {
            throw new BusinessException(400, "状态不能为空");
        }
        Workshop workshop = workshopMapper.selectById(id);
        if (workshop == null) {
            throw new BusinessException(404 , "车间不存在");
        }
        workshop.setStatus(status);
        if (status == WorkshopStatus.DISABLE) {
            ProductionLine productionLine = productionLineMapper.selectOne(new LambdaQueryWrapper<ProductionLine>()
                    .eq(ProductionLine::getWorkshopId , workshop.getId()));
            if (productionLine != null) {
                productionLine.setStatus(ProductionLineStatus.DISABLE);
                productionLineMapper.updateById(productionLine);
            }
        }
        workshopMapper.updateById(workshop);
    }
}
