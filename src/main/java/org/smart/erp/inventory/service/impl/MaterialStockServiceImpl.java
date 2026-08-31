package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.inventory.dto.materialStockDto.CreateDto;
import org.smart.erp.inventory.dto.materialStockDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.mapper.MaterialStockMapper;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.MaterialStockVO;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MaterialStockServiceImpl
        extends ServiceImpl<MaterialStockMapper, MaterialStock>
        implements MaterialStockService
{

    private final MaterialStockMapper materialStockMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final TransactionService transactionService;

    public MaterialStockServiceImpl(MaterialStockMapper materialStockMapper, MaterialMapper materialMapper, WarehouseMapper warehouseMapper, TransactionService transactionService) {
        this.materialStockMapper = materialStockMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.transactionService = transactionService;
    }

    /**
     * 获取物料库存视图对象
     * @param materialStock 物料库存实体对象
     * @param materialId 物料id
     * @param warehouseId 仓库id
     * @return 物料库存视图对象
     */
    @NonNull
    private MaterialStockVO getMaterialStockVO(MaterialStock materialStock, Long materialId, Long warehouseId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(400, "物料不存在");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException(400, "仓库不存在");
        }
        MaterialStockVO vo = new MaterialStockVO();
        BeanUtils.copyProperties(materialStock, vo);
        vo.setAvailable(materialStock.getOnHand().subtract(materialStock.getReserved()));
        vo.setMaterialName(material.getName());
        vo.setMaterialCode(material.getCode());
        vo.setWarehouseName(warehouse.getName());
        return vo;
    }

    /**
     * 获取物料库存查询条件
     * @param materialId 物料id
     * @param warehouseId 仓库id
     * @param quantity 数量
     * @param errorMsg 错误信息
     * @return 物料库存查询条件
     */
    private LambdaQueryWrapper<MaterialStock> buildStockQueryWrapper(
            Long materialId,
            Long warehouseId,
            BigDecimal quantity,
            String errorMsg)
    {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, errorMsg);
        }

        return new LambdaQueryWrapper<MaterialStock>()
                .eq(MaterialStock::getMaterialId, materialId)
                .eq(MaterialStock::getWarehouseId, warehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialStockVO createMaterialStock(CreateDto dto) {
        MaterialStock existing = materialStockMapper.selectOne(new LambdaQueryWrapper<MaterialStock>()
                .eq(MaterialStock::getMaterialId, dto.getMaterialId())
                .eq(MaterialStock::getWarehouseId, dto.getWarehouseId())
        );
        if (existing != null) {
            throw new BusinessException(400, "物料在该仓库已存在库存记录");
        }
        Material material = materialMapper.selectById(dto.getMaterialId());
        if (material == null || material.getStatus().equals(MaterialStatus.DISABLE)) {
            throw new BusinessException(400, "物料不存在或被停用");
        }

        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null || warehouse.getStatus().equals(WarehouseStatus.DISABLE)) {
            throw new BusinessException(400, "仓库不存在或被停用");
        }

        MaterialStock materialStock = new MaterialStock();
        materialStock.setMaterialId(dto.getMaterialId());
        materialStock.setWarehouseId(dto.getWarehouseId());
        materialStock.setOnHand(BigDecimal.ZERO);
        materialStock.setReserved(BigDecimal.ZERO);
        save(materialStock);

        return getMaterialStockVO(materialStock, dto.getMaterialId(), dto.getWarehouseId());
    }

    @Override
    public Page<MaterialStockVO> listMaterialStock(ListDto dto) {
        LambdaQueryWrapper<MaterialStock> queryWrapper =
                new LambdaQueryWrapper<MaterialStock>()
                        .eq(dto.getMaterialId() != null, MaterialStock::getMaterialId, dto.getMaterialId())
                        .eq(dto.getWarehouseId() != null, MaterialStock::getWarehouseId, dto.getWarehouseId());

        // 物料编码模糊过滤：先查匹配的物料 id，再 in
        if (StringUtils.hasText(dto.getMaterialCode())) {
            List<Long> matchedIds = materialMapper.selectList(
                    new LambdaQueryWrapper<Material>().like(Material::getCode, dto.getMaterialCode())
            ).stream().map(Material::getId).toList();
            if (matchedIds.isEmpty()) {
                return new Page<>(dto.getPageNum(), dto.getPageSize());
            }
            queryWrapper.in(MaterialStock::getMaterialId, matchedIds);
        }

        Page<MaterialStock> page = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);

        List<Long> materialIds = page.getRecords().stream().map(MaterialStock::getMaterialId).distinct().toList();
        List<Long> warehouseIds = page.getRecords().stream().map(MaterialStock::getWarehouseId).distinct().toList();

        if (page.getRecords().isEmpty()) {
            return PageConvertUtils.convert(
                    page,
                    stock -> new MaterialStockVO()
            );
        }

        Map<Long, Material> materialMap = materialMapper.selectByIds(materialIds).stream()
                .filter(Objects::nonNull).collect(Collectors.toMap(Material::getId, m -> m));
        Map<Long, String> warehouseNameMap = warehouseMapper.selectByIds(warehouseIds).stream()
                .filter(Objects::nonNull).collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));

        return PageConvertUtils.convert(page, materialStock -> {
            MaterialStockVO vo = new MaterialStockVO();
            BeanUtils.copyProperties(materialStock, vo);
            vo.setAvailable(materialStock.getOnHand().subtract(materialStock.getReserved()));
            Material m = materialMap.get(materialStock.getMaterialId());
            if (m != null) {
                vo.setMaterialName(m.getName());
                vo.setMaterialCode(m.getCode());
            }
            vo.setWarehouseName(warehouseNameMap.get(materialStock.getWarehouseId()));
            return vo;
        });
    }

    @Override
    public MaterialStockVO getMaterialStock(Long id) {
        MaterialStock materialStock = materialStockMapper.selectById(id);
        if (materialStock == null) {
            throw new BusinessException(400, "库存记录不存在");
        }
        return getMaterialStockVO(materialStock, materialStock.getMaterialId(), materialStock.getWarehouseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveStock(Long materialId, Long warehouseId, BigDecimal quantity) {
        LambdaQueryWrapper<MaterialStock> queryWrapper = buildStockQueryWrapper(materialId, warehouseId, quantity, "预留数量必须大于 0");

        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            MaterialStock materialStock = materialStockMapper.selectOne(queryWrapper);
            if (materialStock == null) {
                throw new BusinessException(400, "库存记录不存在");
            }

            BigDecimal available = materialStock.getOnHand().subtract(materialStock.getReserved());
            if (available.compareTo(quantity) < 0) {
                throw new BusinessException(400, "可用库存不足");
            }

            BigDecimal beforeOnHand = materialStock.getOnHand();
            BigDecimal beforeReserved = materialStock.getReserved();

            materialStock.setReserved(materialStock.getReserved().add(quantity));

            if (this.updateById(materialStock)) {
                transactionService.recordTransaction(
                        materialStock,
                        TransactionType.RESERVE,
                        quantity, beforeOnHand,
                        beforeReserved,
                        "预留库存",
                        "预留库存",
                        "预留库存"
                );
                return;
            }
        }

        throw new BusinessException(409, "预留库存失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(Long materialId, Long warehouseId, BigDecimal quantity) {
        LambdaQueryWrapper<MaterialStock> queryWrapper = buildStockQueryWrapper(materialId, warehouseId, quantity, "释放数量必须大于 0");

        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            MaterialStock materialStock = materialStockMapper.selectOne(queryWrapper);
            if (materialStock == null) {
                throw new BusinessException(400, "库存记录不存在");
            }

            if (materialStock.getReserved().compareTo(quantity) < 0) {
                throw new BusinessException(400, "预留库存不足");
            }

            BigDecimal beforeOnHand = materialStock.getOnHand();
            BigDecimal beforeReserved = materialStock.getReserved();

            materialStock.setReserved(materialStock.getReserved().subtract(quantity));

            if (this.updateById(materialStock)) {
                transactionService.recordTransaction(
                        materialStock,
                        TransactionType.RELEASE,
                        quantity, beforeOnHand,
                        beforeReserved,
                        "释放库存",
                        "释放库存",
                        "释放库存"
                );
                return;
            }
        }

        throw new BusinessException(409, "释放库存失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outboundStock(Long materialId, Long warehouseId, BigDecimal quantity) {
        LambdaQueryWrapper<MaterialStock> queryWrapper = buildStockQueryWrapper(materialId, warehouseId, quantity, "出库数量必须大于 0");

        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            MaterialStock materialStock = materialStockMapper.selectOne(queryWrapper);
            if (materialStock == null) {
                throw new BusinessException(400, "库存记录不存在");
            }
            if (quantity.compareTo(materialStock.getOnHand()) > 0) {
                throw new BusinessException(400, "出库数量不能大于在库数量");
            }
            if (quantity.compareTo(materialStock.getReserved()) > 0) {
                throw new BusinessException(400, "出库数量不能大于预留数量");
            }

            BigDecimal beforeOnHand = materialStock.getOnHand();
            BigDecimal beforeReserved = materialStock.getReserved();

            materialStock.setOnHand(materialStock.getOnHand().subtract(quantity));
            materialStock.setReserved(materialStock.getReserved().subtract(quantity));

            if (this.updateById(materialStock)) {
                transactionService.recordTransaction(
                        materialStock,
                        TransactionType.OUTBOUND,
                        quantity, beforeOnHand,
                        beforeReserved,
                        "SALES_ORDER",
                        "SO202608240001",
                        "出库"
                );
                return;
            }
        }

        throw new BusinessException(409, "出库失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inboundStock(Long materialId, Long warehouseId, BigDecimal quantity) {
        LambdaQueryWrapper<MaterialStock> queryWrapper = buildStockQueryWrapper(materialId, warehouseId, quantity, "入库数量必须大于 0");

        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            MaterialStock materialStock = materialStockMapper.selectOne(queryWrapper);
            if (materialStock == null) {
                throw new BusinessException(400, "库存记录不存在");
            }

            BigDecimal beforeOnHand = materialStock.getOnHand();
            BigDecimal beforeReserved = materialStock.getReserved();

            materialStock.setOnHand(materialStock.getOnHand().add(quantity));

            if (this.updateById(materialStock)) {
                transactionService.recordTransaction(
                        materialStock,
                        TransactionType.INBOUND,
                        quantity, beforeOnHand,
                        beforeReserved,
                        "PURCHASE_ORDER",
                        "IO2026082400201",
                        "入库"
                );
                return;
            }
        }

        throw new BusinessException(409, "入库失败，请稍后重试");
    }

}
