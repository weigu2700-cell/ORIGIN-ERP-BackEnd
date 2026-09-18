package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.common.utils.PageConvertUtils;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.dto.PurchaseInStockPageDto;
import org.smart.erp.purchase.dto.PurchaseInStockUploadDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.smart.erp.purchase.mapper.PurchaseInStockMapper;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.smart.erp.production.service.ProductionPickingService;
import org.smart.erp.purchase.vo.PurchaseInStockVo;
import org.smart.erp.eip.dto.NotificationBusinessRefDTO;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.service.NotificationPublisher;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseInStockServiceImpl
    extends ServiceImpl<PurchaseInStockMapper, PurchaseInStock>
    implements PurchaseInStockService
{

    private final Validator validator;
    private final BusinessNoGenerator businessNoGenerator;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final MaterialStockService materialStockService;
    private final ProductionPickingService productionPickingService;
    private final RedissonClient redissonClient;
    private final NotificationPublisher notificationPublisher;

    @Lazy
    @Autowired
    private PurchaseInStockServiceImpl self;

    public PurchaseInStockServiceImpl(
                    Validator validator,
                    BusinessNoGenerator businessNoGenerator,
                    MaterialMapper materialMapper,
                    SupplierMapper supplierMapper,
                    WarehouseMapper warehouseMapper,
                    PurchaseOrderMapper purchaseOrderMapper,
                    MaterialStockService materialStockService,
                    ProductionPickingService productionPickingService,
                    RedissonClient redissonClient,
                    NotificationPublisher notificationPublisher
            ) {
        this.validator = validator;
        this.businessNoGenerator = businessNoGenerator;
        this.materialMapper = materialMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.materialStockService = materialStockService;
        this.productionPickingService = productionPickingService;
        this.redissonClient = redissonClient;
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * 判断对象是否为空，为空则抛出异常
     *
     * @param obj 对象
     */
    private void checkNull(Object obj, String msg) {
        if (obj == null || (obj instanceof String s && s.isBlank())) {
            throw new BusinessException(400, msg);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchaseInStock(PurchaseInStockAddDto dto) {
        checkNull(dto,"入库信息不能为空");

        Set<ConstraintViolation<PurchaseInStockAddDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(400, violations.iterator().next().getMessage());
        }

        PurchaseInStock purchaseInStock = new PurchaseInStock();
        BeanUtils.copyProperties(dto, purchaseInStock);
        purchaseInStock.setInStockNo(businessNoGenerator.generateNo("erp:sequence:purchase-in-stock:", "PI"));
        purchaseInStock.setStatus(PurchaseInStockStatus.DRAFT);
        purchaseInStock.setInType(PurchaseInStockType.PURCHASE_NORMAL);
        if (!this.save(purchaseInStock)) {
            throw new BusinessException(500, "采购入库单创建失败");
        }
        notificationPublisher.publish(NotificationPublishDTO.business(
                NotificationType.TASK, "待审批采购入库单",
                "采购入库单 " + purchaseInStock.getInStockNo() + " 已创建，请及时审批。",
                NotificationBusinessRefDTO.of(
                        "PURCHASE_IN_STOCK_PENDING_APPROVAL",
                        purchaseInStock.getId(), purchaseInStock.getInStockNo()),
                RecipientSelectorDTO.permissions(Set.of("purchase:in:stock:approve"), true)));

    }

    @Override
    public Page<PurchaseInStockVo> getPagePurchaseInStock(PurchaseInStockPageDto queryDto) {
        int pageNum = (queryDto.getPageNum() == null || queryDto.getPageNum() < 1) ? 1 : queryDto.getPageNum();
        int pageSize = (queryDto.getPageSize() == null || queryDto.getPageSize() < 1) ? 10 : queryDto.getPageSize();

        LambdaQueryWrapper<PurchaseInStock> qw = new LambdaQueryWrapper<PurchaseInStock>()
                .like(StringUtils.hasText(queryDto.getPurchaseInStockNo()),
                        PurchaseInStock::getInStockNo, queryDto.getPurchaseInStockNo())
                .like(StringUtils.hasText(queryDto.getPurchaseOrderNo()),
                        PurchaseInStock::getPurchaseOrderNo, queryDto.getPurchaseOrderNo())
                .eq(Objects.nonNull(queryDto.getMaterialId()),
                        PurchaseInStock::getMaterialId, queryDto.getMaterialId())
                .eq(Objects.nonNull(queryDto.getWarehouseId()),
                        PurchaseInStock::getWarehouseId, queryDto.getWarehouseId())
                .eq(StringUtils.hasText(queryDto.getStorageLocation()),
                        PurchaseInStock::getStorageLocation, queryDto.getStorageLocation())
                .eq(StringUtils.hasText(queryDto.getOperator()),
                        PurchaseInStock::getOperator, queryDto.getOperator())
                .eq(Objects.nonNull(queryDto.getInType()),
                        PurchaseInStock::getInType, queryDto.getInType())
                .eq(Objects.nonNull(queryDto.getStatus()),
                        PurchaseInStock::getStatus, queryDto.getStatus())
                .eq(Objects.nonNull(queryDto.getProductionDate()),
                        PurchaseInStock::getProductionDate, queryDto.getProductionDate())
                .eq(Objects.nonNull(queryDto.getDeliveryDate()),
                        PurchaseInStock::getInDate, queryDto.getDeliveryDate())
                .orderByDesc(PurchaseInStock::getCreateTime);

        // 供应商不在入库单表上，先按供应商查采购订单，再反查入库单
        if (queryDto.getSupplierId() != null) {
            List<Long> orderIdsBySupplier = purchaseOrderMapper.selectList(
                            new LambdaQueryWrapper<PurchaseOrder>()
                                    .eq(PurchaseOrder::getSupplierId, queryDto.getSupplierId()))
                    .stream().map(PurchaseOrder::getId).toList();
            if (orderIdsBySupplier.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            qw.in(PurchaseInStock::getPurchaseOrderId, orderIdsBySupplier);
        }

        Page<PurchaseInStock> mpPage =
                this.page(new Page<>(pageNum, pageSize), qw);
        List<PurchaseInStock> records = mpPage.getRecords();

        List<Long> materialIds = records.stream().map(PurchaseInStock::getMaterialId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectByIds(materialIds).stream()
                        .collect(Collectors.toMap(Material::getId, m -> m));

        List<Long> warehouseIds = records.stream().map(PurchaseInStock::getWarehouseId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectByIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<Long> orderIds = records.stream().map(PurchaseInStock::getPurchaseOrderId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> orderSupplierMap = orderIds.isEmpty() ? Map.of()
                : purchaseOrderMapper.selectByIds(orderIds).stream()
                        .filter(order -> order.getSupplierId() != null)
                        .collect(Collectors.toMap(PurchaseOrder::getId, PurchaseOrder::getSupplierId));
        List<Long> supplierIds = orderSupplierMap.values().stream().distinct().toList();
        Map<Long, Supplier> supplierMap = supplierIds.isEmpty() ? Map.of()
                : supplierMapper.selectByIds(supplierIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, s -> s));

        return PageConvertUtils.convert(mpPage, item -> {
            PurchaseInStockVo vo = new PurchaseInStockVo();
            BeanUtils.copyProperties(item, vo);
            vo.setPurchaseInStockNo(item.getInStockNo());

            Material material = materialMap.get(item.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getName());
                vo.setMaterialCode(material.getCode());
            }
            Warehouse warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
                vo.setWarehouseCode(warehouse.getCode());
            }
            Long supplierId = orderSupplierMap.get(item.getPurchaseOrderId());
            vo.setSupplierId(supplierId);
            if (supplierId != null) {
                Supplier supplier = supplierMap.get(supplierId);
                if (supplier != null) {
                    vo.setSupplierName(supplier.getName());
                    vo.setSupplierCode(supplier.getCode());
                }
            }
            return vo;
        });
    }

    private static final String LOCK_PREFIX = "erp:purchase:inStock:lock:";

    @Override
    public void approvePurchaseInStock(Long id) {
        RLock rLock = redissonClient.getLock(LOCK_PREFIX + id);
        boolean locked;
        try {
            locked = rLock.tryLock(5,  TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "获取入库锁被中断");
        }
        if (!locked) {
            throw new BusinessException(409, "入库单正在处理中，请稍后重试");
        }
        try {
            // 经由 self 代理调用，确保 @Transactional 真正生效、DB 事务在解锁前已提交
            self.doApprovePurchaseInStock(id);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void doApprovePurchaseInStock(Long id) {
        PurchaseInStock purchaseInStock = this.getById(id);
        checkNull(purchaseInStock, "入库信息不存在");
        if (purchaseInStock.getStatus() != PurchaseInStockStatus.DRAFT) {
            throw new BusinessException(400, "入库信息状态不为草稿，无法审核");
        }
        purchaseInStock.setStatus(PurchaseInStockStatus.APPROVED);
        if (!this.updateById(purchaseInStock)) {
            throw new BusinessException(409, "采购入库单状态更新失败，请刷新后重试");
        }
        notificationPublisher.publish(NotificationPublishDTO.business(
                NotificationType.TASK, "待上架采购入库单",
                "采购入库单 " + purchaseInStock.getInStockNo() + " 已审批通过，请及时上架。",
                NotificationBusinessRefDTO.of(
                        "PURCHASE_IN_STOCK_PENDING_UPLOAD",
                        purchaseInStock.getId(), purchaseInStock.getInStockNo()),
                RecipientSelectorDTO.permissions(Set.of("purchase:in:stock:upload"), true)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadPurchaseInStock(Long id, PurchaseInStockUploadDto dto) {
        checkNull(dto, "上架信息不能为空");
        Set<ConstraintViolation<PurchaseInStockUploadDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(400, violations.iterator().next().getMessage());
        }

        PurchaseInStock purchaseInStock = this.getById(id);
        checkNull(purchaseInStock, "入库单不存在");

        if (purchaseInStock.getStatus() != PurchaseInStockStatus.APPROVED) {
            throw new BusinessException(400, "入库单状态不为已审核，无法上架");
        }

        if (purchaseInStock.getMaterialId() == null) {
            throw new BusinessException(400, "入库单未关联物料，无法上架");
        }
        if (purchaseInStock.getInQuantity() == null) {
            throw new BusinessException(400, "入库数量未填写，无法上架");
        }

        //仓库：优先用本次指定的，回退到入库单上已有的
        Long warehouseId = dto.getWarehouseId() != null
                ? dto.getWarehouseId()
                : purchaseInStock.getWarehouseId();
        if (warehouseId == null) {
            throw new BusinessException(400, "仓库未指定，无法上架");
        }

        materialStockService.inboundStock(
                purchaseInStock.getMaterialId(),
                warehouseId,
                purchaseInStock.getInQuantity(),
                "PURCHASE_IN_STOCK",
                purchaseInStock.getInStockNo(),
                "采购入库上架"
        );

        purchaseInStock.setWarehouseId(warehouseId);
        purchaseInStock.setStorageLocation(dto.getStorageLocation());
        purchaseInStock.setBatchNo(dto.getBatchNo());
        if (dto.getProductionDate() != null) {
            purchaseInStock.setProductionDate(dto.getProductionDate());
        }
        if (dto.getExpiryDate() != null) {
            purchaseInStock.setExpiryDate(dto.getExpiryDate());
        }
        purchaseInStock.setInDate(LocalDateTime.now());
        purchaseInStock.setStatus(PurchaseInStockStatus.UPLOADED);
        this.updateById(purchaseInStock);

        // 采购入库上架后，通知对应缺料领料单可领料（指定入库仓库并预留）
        if (purchaseInStock.getMaterialId() != null) {
            productionPickingService.notifyPickingForInStock(purchaseInStock.getMaterialId(), warehouseId);
        }
    }

    @Override
    public PurchaseInStockVo getPurchaseInStock(Long id) {
        PurchaseInStock purchaseInStock = this.getById(id);
        checkNull(purchaseInStock, "入库单不存在");

        // 关联数据：外键为空时不查库，避免无意义的查询
        Material material = purchaseInStock.getMaterialId() == null ? null
                : materialMapper.selectById(purchaseInStock.getMaterialId());
        PurchaseOrder purchaseOrder = purchaseInStock.getPurchaseOrderId() == null ? null
                : purchaseOrderMapper.selectById(purchaseInStock.getPurchaseOrderId());
        Warehouse warehouse = purchaseInStock.getWarehouseId() == null ? null
                : warehouseMapper.selectById(purchaseInStock.getWarehouseId());
        // 供应商挂在采购订单上，订单可能为空
        Supplier supplier = (purchaseOrder == null || purchaseOrder.getSupplierId() == null) ? null
                : supplierMapper.selectById(purchaseOrder.getSupplierId());

        PurchaseInStockVo vo = new PurchaseInStockVo();
        BeanUtils.copyProperties(purchaseInStock, vo);
        vo.setPurchaseInStockNo(purchaseInStock.getInStockNo());

        Optional.ofNullable(material).ifPresent(m -> {
            vo.setMaterialName(m.getName());
            vo.setMaterialCode(m.getCode());
        });

        Optional.ofNullable(warehouse).ifPresent(w -> {
            vo.setWarehouseName(w.getName());
            vo.setWarehouseCode(w.getCode());
        });

        Optional.ofNullable(supplier).ifPresent(s -> {
            vo.setSupplierId(s.getId());
            vo.setSupplierName(s.getName());
            vo.setSupplierCode(s.getCode());
        });

        checkNull(purchaseOrder, "采购订单不存在");
        vo.setPurchaseOrderNo(purchaseOrder.getPurchaseOrderNo());
        return vo;
    }

}
