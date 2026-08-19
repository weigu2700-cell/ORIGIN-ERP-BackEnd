package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.DateCodeRuleUtil;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineCreateDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineListDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineUpdateDTO;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.entity.Workshop;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.mapper.ProductionLineMapper;
import org.smart.erp.master.mapper.WorkshopMapper;
import org.smart.erp.master.service.ProductionLineService;
import org.smart.erp.master.vo.ProductionLineVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductionLineServiceImpl
        extends ServiceImpl<ProductionLineMapper, ProductionLine>
        implements ProductionLineService
{

    private final WorkshopMapper workshopMapper;
    private final DateCodeRuleUtil dateCodeRuleUtil;

    public ProductionLineServiceImpl(WorkshopMapper workshopMapper, DateCodeRuleUtil dateCodeRuleUtil) {
        this.workshopMapper = workshopMapper;
        this.dateCodeRuleUtil = dateCodeRuleUtil;
    }

    /**
     * 获取生产线，并校验所属车间是否存在或已停用。
     * @param id 生产线 id
     * @return 生产线
     */
    private ProductionLine getProductionLineOrThrow(Long id) {
        ProductionLine productionLine = getById(id);
        if (productionLine == null) {
            throw new BusinessException(404, "生产线不存在");
        }
        Workshop workshop = workshopMapper.selectById(productionLine.getWorkshopId());
        if (workshop == null || workshop.getStatus().equals(WorkshopStatus.DISABLE)) {
            throw new BusinessException(404, "所属车间不存在或已停用");
        }
        return productionLine;
    }
    /**
     * 将生产线实体转换为详情 VO。
     * @param productionLine 生产线实体
     * @param workshopName 所属车间名称
     * @return 详情 VO
     */
    private ProductionLineVO toDetailVO(ProductionLine productionLine, String workshopName) {
        ProductionLineVO vo = new ProductionLineVO();
        BeanUtils.copyProperties(productionLine, vo);
        vo.setWorkshopName(workshopName);
        return vo;
    }


    @Override
    public void createProductionLine(ProductionLineCreateDTO dto) {
        ProductionLine productionLine = new ProductionLine();
        Workshop workshop = workshopMapper.selectById(dto.getWorkshopId());
        if (workshop == null || workshop.getStatus().equals(WorkshopStatus.DISABLE)) {
            throw new BusinessException(400 , "所属车间不存在或已停用");
        }
        BeanUtils.copyProperties(dto, productionLine);
        productionLine.setCode(dateCodeRuleUtil.setDateCodeRule("PL" , workshop.getId()));
        productionLine.setStatus(ProductionLineStatus.ENABLE);
        save(productionLine);
    }

    @Override
    public Page<ProductionLineVO> listProductionLine(ProductionLineListDTO dto) {
        LambdaQueryWrapper<ProductionLine> queryWrapper =
                new LambdaQueryWrapper<ProductionLine>()
                        .eq(dto.getWorkshopId() != null, ProductionLine::getWorkshopId, dto.getWorkshopId())
                        .eq(dto.getStatus() != null, ProductionLine::getStatus, dto.getStatus())
                        .like(dto.getName() != null, ProductionLine::getName, dto.getName());

        Page<ProductionLine> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        List<Long> workshopIds = page.getRecords().stream()
                .map(ProductionLine::getWorkshopId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> workshopNameMap = workshopIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : workshopMapper.selectByIds(workshopIds).stream()
                        .collect(Collectors.toMap(Workshop::getId, Workshop::getName));

        return PageConvertUtils.convert(page, line -> {
            ProductionLineVO vo = new ProductionLineVO();
            BeanUtils.copyProperties(line, vo);
            vo.setWorkshopName(workshopNameMap.get(line.getWorkshopId()));
            return vo;
        });
    }

    @Override
    public ProductionLineVO getProductionLine(Long id) {
        ProductionLine productionLine = getProductionLineOrThrow(id);
        Workshop workshop = workshopMapper.selectById(productionLine.getWorkshopId());
        return toDetailVO(productionLine, workshop.getName());
    }

    @Override
    public void updateProductionLine(Long id, ProductionLineUpdateDTO dto) {
        ProductionLine productionLine = getProductionLineOrThrow(id);

        BeanUtils.copyProperties(dto, productionLine);
        updateById(productionLine);
    }

    @Override
    public void updateProductionLineStatus(Long id, ProductionLineStatus status) {
        ProductionLine productionLine = getProductionLineOrThrow(id);
        productionLine.setStatus(status);
        updateById(productionLine);
    }




}
