package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.mapper.MaterialStockMapper;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.dto.createBOMItemDto;
import org.smart.erp.production.dto.pageBOMDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.entity.BOMItem;
import org.smart.erp.production.enums.BOMStatus;
import org.smart.erp.production.mapper.BOMItemMapper;
import org.smart.erp.production.mapper.BOMMapper;
import org.smart.erp.production.service.BOMItemService;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.vo.BOMExplosionVo;
import org.smart.erp.production.vo.BOMItemVo;
import org.smart.erp.production.vo.BOMVo;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BOMServiceImpl
        extends ServiceImpl<BOMMapper, BOM>
        implements BOMService
{

    private final MaterialMapper materialMapper;
    private final BOMMapper bomMapper;
    private final BOMItemMapper bomItemMapper;
    private final BOMItemService bomItemService;
    private final BusinessNoGenerator businessNoGenerator;
    private final MaterialStockMapper materialStockMapper;

    public BOMServiceImpl(
            MaterialMapper materialMapper,
            BOMItemMapper bomItemMapper,
            BOMItemService bomItemService,
            BusinessNoGenerator businessNoGenerator,
            BOMMapper bomMapper,
            MaterialStockMapper materialStockMapper
    )
    {
        this.materialMapper = materialMapper;
        this.bomItemMapper = bomItemMapper;
        this.bomItemService = bomItemService;
        this.businessNoGenerator = businessNoGenerator;
        this.bomMapper = bomMapper;
        this.materialStockMapper = materialStockMapper;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBOM(creatBOMDto dto) {

        Material material = materialMapper.selectById(dto.getMaterialId());

        if (Objects.isNull(material) || material.getStatus().equals(MaterialStatus.DISABLE)) {
            throw new BusinessException(404,"物料不存在或已被禁用");
        }
        if (dto.getBomItems() == null || dto.getBomItems().isEmpty()) {
            throw new BusinessException(400, "BOM 明细不能为空");
        }

        Set<Long> componentMaterialIds = new HashSet<>();
        for (createBOMItemDto bomItemDto : dto.getBomItems()) {
            if (bomItemDto.getComponentMaterialId() == null) {
                throw new BusinessException(400, "组成物料不能为空");
            }
            if (!componentMaterialIds.add(bomItemDto.getComponentMaterialId())) {
                throw new BusinessException(400, "BOM中存在重复组成物料");
            }
        }

        BOM latestBom = baseMapper.selectOne(
                new LambdaQueryWrapper<BOM>()
                        .eq(BOM::getMaterialId, dto.getMaterialId())
                        .orderByDesc(BOM::getVersion)
                        .last("limit 1")
        );
        int version = latestBom == null ? 1 : latestBom.getVersion() + 1;

        BOM bom = new BOM();
        bom.setBomNo(businessNoGenerator.generateNo("erp:sequence:bom:", "BOM"));
        bom.setMaterialId(dto.getMaterialId());
        bom.setStatus(BOMStatus.DRAFT);
        bom.setVersion(version);
        baseMapper.insert(bom);

        int lineNo = 10;
        for (createBOMItemDto bomItemDto : dto.getBomItems()) {
            bomItemService.createBOMItem(bomItemDto, bom.getId(),lineNo);
            lineNo += 10;
        }
    }

    @Override
    public BOMVo getBOMDetailById(Long id) {
        BOM bom = baseMapper.selectById(id);
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }

        Material material = materialMapper.selectById(bom.getMaterialId());

        BOMVo vo = new BOMVo();
        BeanUtils.copyProperties(bom,vo);
        vo.setMaterialCode(material.getCode());
        vo.setMaterialName(material.getName());
        vo.setBomItems(bomItemService.getBOMItemList(bom.getId()));
        return vo;
    }

    @Override
    public Page<BOMVo> getPageBOMVo(pageBOMDto dto) {
        LambdaQueryWrapper<BOM> queryWrapper =
                new LambdaQueryWrapper<BOM>()
                        .like(StringUtils.hasText(dto.getBomNo()),BOM::getBomNo,dto.getBomNo())
                        .eq(Objects.nonNull(dto.getMaterialId()),BOM::getMaterialId,dto.getMaterialId())
                        .eq(Objects.nonNull(dto.getStatus()),BOM::getStatus,dto.getStatus());

        Page<BOM> page = this.page(new Page<>(dto.getPageNum(),dto.getPageSize()),queryWrapper);

        List<Long> bomIds = page.getRecords().stream().map(BOM::getId).toList();
        Map<Long, List<BOMItemVo>> bomItemMap = bomItemService.getBOMItemMap(bomIds);

        // 批量反查 BOM 头所属物料，补全编码与名称（避免逐行查库）
        List<Long> materialIds = page.getRecords().stream()
                .map(BOM::getMaterialId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Material> materialMap = materialIds.isEmpty()
                ? Collections.emptyMap()
                : materialMapper.selectByIds(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        Page<BOMVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(bom -> {
            BOMVo bomVo = new BOMVo();
            BeanUtils.copyProperties(bom, bomVo);
            Material material = materialMap.get(bom.getMaterialId());
            if (material != null) {
                bomVo.setMaterialCode(material.getCode());
                bomVo.setMaterialName(material.getName());
            }
            bomVo.setBomItems(bomItemMap.get(bom.getId()));
            return bomVo;
        }).toList());

        return voPage;
    }

    @Override
    public void disableBOM(Long id) {
        BOM bom = baseMapper.selectById(id);
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }
        if (bom.getStatus() != BOMStatus.ACTIVE) {
            throw new BusinessException(400, "仅启用状态BOM允许停用");
        }
        bom.setStatus(BOMStatus.INACTIVE);
        baseMapper.updateById(bom);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activeBOM(Long id) {
        BOM bom = baseMapper.selectById(id);
        if (Objects.isNull(bom)) {
            throw new BusinessException(404,"BOM不存在");
        }
        if (bom.getStatus().equals(BOMStatus.ACTIVE)) {
            throw new BusinessException(400,"BOM已激活");
        }

        BOM latestBom = baseMapper.selectOne(
                new LambdaQueryWrapper<BOM>()
                        .eq(BOM::getMaterialId, bom.getMaterialId())
                        .orderByDesc(BOM::getVersion)
                        .last("limit 1")
        );
        if (latestBom != null && latestBom.getVersion() >= bom.getVersion()) {
            throw new BusinessException(400,"BOM版本号必须大于当前物料的BOM版本号");
        }
        List<BOMItem> bomItems = bomItemMapper.selectList(
                new LambdaQueryWrapper<BOMItem>().eq(BOMItem::getBomId, bom.getId())
        );
        if (bomItems.isEmpty()) {
            throw new BusinessException(400,"BOM明细不能为空");
        }
        bom.setStatus(BOMStatus.ACTIVE);
        baseMapper.updateById(bom);
    }

    @Override
    public List<BOMExplosionVo> getBOMExplosion(Long materialId, BigDecimal quantity) {
        if (materialId == null) {
            throw new BusinessException(400, "物料ID不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "展开数量必须大于0");
        }
        List<BOMExplosionVo> result = explodeBOM(materialId, quantity, 1, new HashSet<>(), true);
        enrichExplosionMaterial(result);
        return result;
    }

    /** 批量反查组件物料，补全编码与名称（避免逐行查库） */
    private void enrichExplosionMaterial(List<BOMExplosionVo> vos) {
        Set<Long> ids = vos.stream()
                .map(BOMExplosionVo::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Material> materialMap = materialMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));
        vos.forEach(vo -> {
            Material material = materialMap.get(vo.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }
        });
    }

    /**
     * 分解 BOM（纯结构展开，不考虑库存）
     * @param level  当前层级（直接子件为 1）
     * @param path   递归路径，用于循环引用检测
     * @param isRoot 是否顶层入口（仅顶层缺 BOM 才报错）
     */
    private List<BOMExplosionVo> explodeBOM(
            Long materialId, BigDecimal quantity, int level, Set<Long> path, boolean isRoot) {
        // 循环引用保护，避免无限递归
        if (!path.add(materialId)) {
            throw new BusinessException(400, "BOM存在循环引用");
        }
        BOM parentBom = bomMapper.selectOne(
                new LambdaQueryWrapper<BOM>()
                        .eq(BOM::getMaterialId, materialId)
                        .eq(BOM::getStatus, BOMStatus.ACTIVE)
                        .last("limit 1"));
        // 叶子节点（如采购件）无 BOM 属正常情况，仅顶层入口才报错
        if (Objects.isNull(parentBom)) {
            if (isRoot) {
                throw new BusinessException(404, "BOM不存在");
            }
            return Collections.emptyList();
        }
        List<BOMItem> bomItems = bomItemMapper.selectList(
                new LambdaQueryWrapper<BOMItem>().eq(BOMItem::getBomId, parentBom.getId()));

        List<BOMExplosionVo> result = new ArrayList<>();
        for (BOMItem bomItem : bomItems) {
            BOMExplosionVo vo = new BOMExplosionVo();
            BeanUtils.copyProperties(bomItem, vo);
            vo.setMaterialId(bomItem.getComponentMaterialId());
            BigDecimal unitQty = bomItem.getQuantity() == null ? BigDecimal.ZERO : bomItem.getQuantity();
            BigDecimal lossRate = bomItem.getLossRate() == null ? BigDecimal.ZERO : bomItem.getLossRate();
            BigDecimal explodedQty = unitQty.multiply(quantity).multiply(BigDecimal.ONE.add(lossRate));
            vo.setQuantity(explodedQty);
            vo.setLevel(level);
            result.add(vo);
            result.addAll(explodeBOM(
                    bomItem.getComponentMaterialId(), explodedQty, level + 1, new HashSet<>(path), false));
        }
        return result;
    }

    @Override
    public List<MaterialRequirementVo> calculateMaterialRequirement(Long materialId, BigDecimal quantity) {
        if (materialId == null) {
            throw new BusinessException(400, "物料ID不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "需求数量必须大于0");
        }

        // 第一阶段：预扫描整棵 BOM 树，收集组件物料并缓存 BOM 明细（用于一次性批量查库存）
        Set<Long> componentIds = new HashSet<>();
        Map<Long, List<BOMItem>> bomItemsCache = new HashMap<>();
        collectBomTree(materialId, new HashSet<>(), componentIds, bomItemsCache);

        // 顶层物料必须存在 ACTIVE BOM
        if (!bomItemsCache.containsKey(materialId)) {
            throw new BusinessException(404, "BOM不存在");
        }

        // 第二阶段：一次查询所有涉及物料库存，构建共享库存池（materialId -> 剩余可用量）
        Map<Long, BigDecimal> remainingStock = loadRemainingStock(componentIds);

        // 第三阶段：净需求递归（只有 shortage > 0 才展开下一层）
        List<MaterialRequirementVo> result = calculateRequirementRecursive(
                materialId, quantity, new HashSet<>(), remainingStock, bomItemsCache);

        // 第四阶段：批量补物料编码/名称
        enrichMaterialRequirement(result);
        return result;
    }

    /**
     * 预扫描整棵 BOM 树。visited 仅保证每个物料只扫描一次（如 A→B→D 与 A→C→D 中 D 只加载一次）；
     * 真正的循环 BOM 检测由 calculateRequirementRecursive 的 path 完成。
     */
    private void collectBomTree(Long materialId, Set<Long> visited,
                                Set<Long> componentIds, Map<Long, List<BOMItem>> bomItemsCache) {
        // 已扫描过则跳过
        if (!visited.add(materialId)) {
            return;
        }
        BOM bom = bomMapper.selectOne(
                new LambdaQueryWrapper<BOM>()
                        .eq(BOM::getMaterialId, materialId)
                        .eq(BOM::getStatus, BOMStatus.ACTIVE)
                        .last("limit 1"));
        // 无 ACTIVE BOM：采购件/原材料，作为叶子节点结束
        if (Objects.isNull(bom)) {
            return;
        }
        List<BOMItem> bomItems = bomItemMapper.selectList(
                new LambdaQueryWrapper<BOMItem>()
                        .eq(BOMItem::getBomId, bom.getId())
                        .orderByAsc(BOMItem::getLineNo));
        bomItemsCache.put(materialId, bomItems);
        for (BOMItem bomItem : bomItems) {
            Long componentId = bomItem.getComponentMaterialId();
            if (componentId == null) {
                continue;
            }
            componentIds.add(componentId);
            collectBomTree(componentId, visited, componentIds, bomItemsCache);
        }
    }

    /**
     * 净需求递归：毛需求 → 库存抵扣 → 净需求/shortage → 仅 shortage>0 才用 shortage 继续展开该组件 BOM。
     * remainingStock 为共享库存池，按遇到顺序扣减，同一库存不会被多个 BOM 路径重复使用。
     */
    private List<MaterialRequirementVo> calculateRequirementRecursive(
            Long materialId, BigDecimal parentQuantity, Set<Long> path,
            Map<Long, BigDecimal> remainingStock, Map<Long, List<BOMItem>> bomItemsCache) {
        // path 为当前递归路径，第二次进入同一物料即发现循环引用
        if (!path.add(materialId)) {
            throw new BusinessException(400, "BOM存在循环引用");
        }
        List<BOMItem> bomItems = bomItemsCache.get(materialId);
        // 当前物料无 BOM：叶子物料，结束
        if (bomItems == null || bomItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<MaterialRequirementVo> result = new ArrayList<>();
        for (BOMItem bomItem : bomItems) {
            Long componentId = bomItem.getComponentMaterialId();
            if (componentId == null) {
                continue;
            }
            BigDecimal unitQuantity = bomItem.getQuantity() == null ? BigDecimal.ZERO : bomItem.getQuantity();
            BigDecimal lossRate = bomItem.getLossRate() == null ? BigDecimal.ZERO : bomItem.getLossRate();
            // 毛需求 = 父级需求 × 单位用量 × (1 + 损耗率)
            BigDecimal grossQuantity = parentQuantity.multiply(unitQuantity).multiply(BigDecimal.ONE.add(lossRate));
            // 当前该物料剩余可分配库存
            BigDecimal availableStock = remainingStock.getOrDefault(componentId, BigDecimal.ZERO);
            // 实际用库存抵扣的量 = min(毛需求, 剩余库存)
            BigDecimal stockUsed = grossQuantity.min(availableStock);
            // 净需求 = 毛需求 - 库存抵扣
            BigDecimal shortageQuantity = grossQuantity.subtract(stockUsed);
            // 更新共享库存池（同一库存不被多个分支重复使用）
            remainingStock.put(componentId, availableStock.subtract(stockUsed));

            MaterialRequirementVo vo = new MaterialRequirementVo();
            vo.setMaterialId(componentId);
            vo.setGrossQuantity(grossQuantity);
            vo.setStockUsedAvailableQuantity(stockUsed);
            vo.setShortageQuantity(shortageQuantity);
            result.add(vo);

            // 仅存在净需求才展开该组件自己的 BOM，且传下去的是 shortage 而非 gross
            if (shortageQuantity.compareTo(BigDecimal.ZERO) > 0) {
                result.addAll(calculateRequirementRecursive(
                        componentId, shortageQuantity, new HashSet<>(path), remainingStock, bomItemsCache));
            }
        }
        return result;
    }

    /**
     * 一次性查询所有涉及物料的库存。可用量 = OnHand - Reserved，多仓库汇总（防御性取 max(0) 避免负可用）。
     */
    private Map<Long, BigDecimal> loadRemainingStock(Set<Long> materialIds) {
        Map<Long, BigDecimal> remaining = new HashMap<>();
        if (materialIds == null || materialIds.isEmpty()) {
            return remaining;
        }
        List<MaterialStock> stocks = materialStockMapper.selectList(
                new LambdaQueryWrapper<MaterialStock>().in(MaterialStock::getMaterialId, materialIds));
        for (MaterialStock stock : stocks) {
            Long materialId = stock.getMaterialId();
            if (materialId == null) {
                continue;
            }
            BigDecimal onHand = stock.getOnHand() == null ? BigDecimal.ZERO : stock.getOnHand();
            BigDecimal reserved = stock.getReserved() == null ? BigDecimal.ZERO : stock.getReserved();
            BigDecimal available = onHand.subtract(reserved).max(BigDecimal.ZERO);
            remaining.merge(materialId, available, BigDecimal::add);
        }
        return remaining;
    }

    /** 批量补充物料编码和物料名称（避免逐行查库导致的 N+1） */
    private void enrichMaterialRequirement(List<MaterialRequirementVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> materialIds = vos.stream()
                .map(MaterialRequirementVo::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (materialIds.isEmpty()) {
            return;
        }
        Map<Long, Material> materialMap = materialMapper.selectByIds(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        for (MaterialRequirementVo vo : vos) {
            Material material = materialMap.get(vo.getMaterialId());
            if (material == null) {
                continue;
            }
            vo.setMaterialCode(material.getCode());
            vo.setMaterialName(material.getName());
        }
    }
}
