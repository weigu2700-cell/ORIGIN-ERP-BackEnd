package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.purchase.dto.CreatePurchaseInStockDto;
import org.smart.erp.purchase.dto.PagePurchaseInStockDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.smart.erp.purchase.mapper.PurchaseInStockMapper;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.smart.erp.purchase.vo.PurchaseInStockVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseInStockServiceImpl
    extends ServiceImpl<PurchaseInStockMapper, PurchaseInStock>
    implements PurchaseInStockService
{

    private final PurchaseInStockMapper purchaseInStockMapper;
    private final Validator validator;
    private final BusinessNoGenerator businessNoGenerator;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    public PurchaseInStockServiceImpl(PurchaseInStockMapper purchaseInStockMapper,
                                      Validator validator,
                                      BusinessNoGenerator businessNoGenerator,
                                      MaterialMapper materialMapper,
                                      SupplierMapper supplierMapper,
                                      WarehouseMapper warehouseMapper,
                                      PurchaseOrderMapper purchaseOrderMapper) {
        this.purchaseInStockMapper = purchaseInStockMapper;
        this.validator = validator;
        this.businessNoGenerator = businessNoGenerator;
        this.materialMapper = materialMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    /**
     * 判断对象是否为空，为空则抛出异常
     * @param obj 对象
     * @param msg 异常信息
     */
    private void checkNull(Object obj, String msg) {
        if (obj == null || (obj instanceof String s && s.isBlank())) {
            throw new BusinessException(400, msg);
        }
    }

    @Override
    public void createPurchaseInStock(CreatePurchaseInStockDto dto) {
        checkNull(dto, "入库信息不能为空");

        // 只校验 DTO 上标注了约束的字段，未标注的（备注、库位、各日期等）允许为空
        Set<ConstraintViolation<CreatePurchaseInStockDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(400, violations.iterator().next().getMessage());
        }

        PurchaseInStock purchaseInStock = new PurchaseInStock();
        BeanUtils.copyProperties(dto, purchaseInStock);
        purchaseInStock.setInStockNo(businessNoGenerator.generateNo("erp:sequence:purchase-in-stock:", "PI"));
        purchaseInStock.setStatus(PurchaseInStockStatus.DRAFT);
        purchaseInStock.setInType(PurchaseInStockType.PURCHASE_NORMAL);
        this.save(purchaseInStock);

    }

    @Override
    public Page<PurchaseInStockVo> getPagePurchaseInStock(PagePurchaseInStockDto queryDto) {
        int pageNum = (queryDto.getPageNum() == null || queryDto.getPageNum() < 1) ? 1 : queryDto.getPageNum();
        int pageSize = (queryDto.getPageSize() == null || queryDto.getPageSize() < 1) ? 10 : queryDto.getPageSize();

        LambdaQueryWrapper<PurchaseInStock> qw = new LambdaQueryWrapper<PurchaseInStock>()
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
                .eq(Objects.nonNull(queryDto.getProductionDate()),
                        PurchaseInStock::getProductionDate, queryDto.getProductionDate())
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

        // 物料名称/编码
        List<Long> materialIds = records.stream().map(PurchaseInStock::getMaterialId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectByIds(materialIds).stream()
                        .collect(Collectors.toMap(Material::getId, m -> m));

        // 仓库名称/编码
        List<Long> warehouseIds = records.stream().map(PurchaseInStock::getWarehouseId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectByIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, w -> w));

        // 供应商名称/编码：入库单无 supplierId，经采购订单反查
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
}
