package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.DateCodeRuleUtil;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseCreateDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseListDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseUpdateDTO;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.mapper.FactoryMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.master.service.WarehouseService;
import org.smart.erp.master.vo.WarehouseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {



    private final WarehouseMapper warehouseMapper;
    private final FactoryMapper factoryMapper;
    private final DateCodeRuleUtil dateCodeRuleUtil;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper, FactoryMapper factoryMapper, DateCodeRuleUtil dateCodeRuleUtil) {
        this.warehouseMapper = warehouseMapper;
        this.factoryMapper = factoryMapper;
        this.dateCodeRuleUtil = dateCodeRuleUtil;
    }


    /**
     * 根据工厂 ID 查询工厂，要求工厂存在且处于启用状态。
     * @param factoryId 工厂 ID
     * @return 启用的工厂实体
     */
    private Factory requireEnabledFactory(Long factoryId) {
        Factory factory = factoryMapper.selectById(factoryId);
        if (factory == null || factory.getStatus().equals(FactoryStatus.DISABLE)) {
            throw new BusinessException(400, "所属工厂不存在或已停用");
        }
        return factory;
    }

    /**
     * 检查仓库是否存在，并返回仓库实体。
     * @param id 仓库 ID
     * @return 仓库实体
     */
    private Warehouse getCheckedEntity(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(400, "仓库不存在");
        }
        requireEnabledFactory(warehouse.getFactoryId());
        return warehouse;
    }

    /**
     * 将仓库实体转换为详情 VO。
     * @param warehouse 仓库实体
     * @return 详情 VO
     */
    private WarehouseVO toVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        BeanUtils.copyProperties(warehouse, vo);
        return vo;
    }

    @Override
    public void create(WarehouseCreateDTO dto) {
        if (dto == null || dto.getFactoryId() == null) {
            throw new BusinessException(400, "请选择所属工厂");
        }
        Warehouse warehouse = new Warehouse();
        Factory factory = requireEnabledFactory(dto.getFactoryId());
        BeanUtils.copyProperties(dto, warehouse);
        warehouse.setCode(dateCodeRuleUtil.setDateCodeRule("WH", factory.getId()));
        warehouse.setStatus(WarehouseStatus.ENABLE);
        save(warehouse);
    }

    @Override
    public Page<WarehouseVO> getWarehouseList(WarehouseListDTO dto) {
        LambdaQueryWrapper<Warehouse> queryWrapper =
                new LambdaQueryWrapper<Warehouse>()
                        .eq(dto.getFactoryId() != null, Warehouse::getFactoryId, dto.getFactoryId())
                        .like(dto.getName() != null, Warehouse::getName, dto.getName())
                        .eq(dto.getStatus() != null, Warehouse::getStatus, dto.getStatus())
                        .eq(dto.getType() != null, Warehouse::getType, dto.getType());

        Page<Warehouse> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        List<Long> factoryIds =
                page.getRecords()
                        .stream()
                        .filter(warehouse -> warehouse.getStatus().equals(WarehouseStatus.ENABLE))
                        .map(Warehouse::getFactoryId)
                        .distinct()
                        .toList();

        Map<Long, String> factoryNameMap;

        if (factoryIds.isEmpty()) {
            factoryNameMap = Map.of();
        } else {
            factoryNameMap = factoryMapper.selectByIds(factoryIds)
                    .stream()
                    .collect(Collectors.toMap(Factory::getId, Factory::getName));
        }


        return PageConvertUtils.convert(page, warehouse -> {
            WarehouseVO vo = new WarehouseVO();
            BeanUtils.copyProperties(warehouse, vo);
            vo.setFactoryName(factoryNameMap.get(warehouse.getFactoryId()));
            return vo;
        });
    }

    @Override
    public WarehouseVO getWarehouse(Long id) {
        Warehouse warehouse = getCheckedEntity(id);
        Factory factory = factoryMapper.selectById(warehouse.getFactoryId());
        WarehouseVO vo = toVO(warehouse);
        if (factory != null) {
            vo.setFactoryName(factory.getName());
        }
        return vo;
    }

    @Override
    public void updateWarehouse(Long id, WarehouseUpdateDTO dto) {
        Warehouse warehouse = getCheckedEntity(id);

        if (dto.getName() != null) warehouse.setName(dto.getName());
        if (dto.getAddress() != null) warehouse.setAddress(dto.getAddress());
        if (dto.getRemark() != null) warehouse.setRemark(dto.getRemark());
        updateById(warehouse);
    }

    @Override
    public void updateWarehouseStatus(Long id, WarehouseStatus status) {
        Warehouse warehouse = getCheckedEntity(id);
        warehouse.setStatus(status);
        updateById(warehouse);
    }
}
