package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.purchase.dto.PurchaseOrderAddDto;
import org.smart.erp.purchase.dto.PurchaseOrderPageDto;
import org.smart.erp.purchase.dto.PurchaseOrderUpdateDto;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderServiceImpl
        extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    private final BusinessNoGenerator businessNoGenerator;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseDemandMapper purchaseDemandMapper;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseInStockService purchaseInStockService;

    public PurchaseOrderServiceImpl(
            BusinessNoGenerator businessNoGenerator,
            PurchaseOrderMapper purchaseOrderMapper,
            PurchaseDemandMapper purchaseDemandMapper,
            MaterialMapper materialMapper,
            SupplierMapper supplierMapper,
            PurchaseInStockService purchaseInStockService
    ) {
        this.businessNoGenerator = businessNoGenerator;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseDemandMapper = purchaseDemandMapper;
        this.materialMapper = materialMapper;
        this.supplierMapper = supplierMapper;
        this.purchaseInStockService = purchaseInStockService;
    }

    /**
     * 如果为空则抛异常
     * @param value 值
     * @param message 异常消息
     * @param <T> 值类型
     */
    private static <T> void require(T value, String message) {
        Optional.ofNullable(value).orElseThrow(() -> new BusinessException(400, message));
    }

    /**
     * 将采购订单转换为VO
     * @param orders 采购订单列表
     * @return 采购订单VO列表
     */
    private List<PurchaseOrderVo> toVoList(List<PurchaseOrder> orders) {
        Set<Long> materialIds = orders.stream().map(PurchaseOrder::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> supplierIds = orders.stream().map(PurchaseOrder::getSupplierId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> demandIds = orders.stream().map(PurchaseOrder::getPurchaseDemandId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Material> materialMap = materialIds.isEmpty() ? Collections.emptyMap()
                : materialMapper.selectByIds(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));

        Map<Long, Supplier> supplierMap = supplierIds.isEmpty() ? Collections.emptyMap()
                : supplierMapper.selectByIds(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));

        Map<Long, PurchaseDemand> demandMap = demandIds.isEmpty() ? Collections.emptyMap()
                : purchaseDemandMapper.selectByIds(demandIds).stream()
                .collect(Collectors.toMap(PurchaseDemand::getId, Function.identity()));

        List<PurchaseOrderVo> vos = new ArrayList<>();

        for (PurchaseOrder o : orders) {
            PurchaseOrderVo vo = new PurchaseOrderVo();
            BeanUtils.copyProperties(o, vo);

            Material material = materialMap.get(o.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }

            Supplier supplier = supplierMap.get(o.getSupplierId());
            if (supplier != null) {
                vo.setSupplierCode(supplier.getCode());
                vo.setSupplierName(supplier.getName());
            }

            PurchaseDemand demand = demandMap.get(o.getPurchaseDemandId());
            if (demand != null) {
                vo.setPurchaseDemandNo(demand.getPurchaseDemandNo());
            }

            vos.add(vo);
        }
        return vos;
    }

    /**
     * 状态转换
     * @param id 采购订单ID
     * @param expected 期望状态
     * @param next 下一个状态
     * @param errorMsg 错误消息
     */
    private void transition(Long id, PurchaseOrderStatus expected, PurchaseOrderStatus next, String errorMsg) {
        PurchaseOrder order = getOrderOrThrow(id);
        if (order.getStatus() != expected) {
            throw new BusinessException(400, errorMsg);
        }
        order.setStatus(next);
        purchaseOrderMapper.updateById(order);
    }

    /**
     * 获取采购订单，如果不存在则抛出异常
     * @param id 采购订单ID
     * @return 采购订单
     */
    private PurchaseOrder getOrderOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException(400, "采购订单ID不能为空");
        }
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "采购订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchaseOrder(PurchaseOrderAddDto dto) {
        require(dto.getPurchaseDemandId(), "采购需求ID不能为空");
        require(dto.getMaterialId(), "物料ID不能为空");
        require(dto.getSupplierId(), "供应商ID不能为空");
        require(dto.getPlannedQuantity(), "计划数量不能为空");
        require(dto.getUnitPrice(), "单价不能为空");
        require(dto.getExpectedDeliveryDate(), "预计交货日期不能为空");

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        BeanUtils.copyProperties(dto, purchaseOrder);
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        purchaseOrder.setPurchaseOrderNo(businessNoGenerator.generateNo(
                "erp:purchase:order:no", "PUR_ORD"));
        purchaseOrder.setOrderDate(LocalDateTime.now());
        purchaseOrder.setTotalAmount(
                dto.getUnitPrice()
                        .multiply(dto.getPlannedQuantity()).setScale(2, RoundingMode.HALF_UP));

        purchaseOrderMapper.insert(purchaseOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchaseOrderFromDemand(Long purchaseDemandId) {
        PurchaseDemand demand = purchaseDemandMapper.selectById(purchaseDemandId);
        if (demand == null) {
            throw new BusinessException(400, "采购需求不存在");
        }
        if (demand.getStatus() != PurchaseDemandStatus.DRAFT) {
            throw new BusinessException(400, "采购需求状态不为草稿，无法生成采购订单");
        }
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseDemandId(demand.getId());
        order.setMaterialId(demand.getMaterialId());
        order.setPlannedQuantity(demand.getPurchaseQuantity());
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.setPurchaseOrderNo(businessNoGenerator.generateNo("erp:purchase:order:no", "PUR_ORD"));
        order.setOrderDate(LocalDateTime.now());
        // 供应商 / 单价 / 预计交货日期由采购员在审批前补全
        purchaseOrderMapper.insert(order);
    }

    @Override
    public Page<PurchaseOrderVo> pagePurchaseOrder(PurchaseOrderPageDto dto) {
        int pageNum = dto.getPageNum() == null ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();

        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<PurchaseOrder>()
                .eq(dto.getMaterialId() != null, PurchaseOrder::getMaterialId, dto.getMaterialId())
                .eq(dto.getSupplierId() != null, PurchaseOrder::getSupplierId, dto.getSupplierId())
                .eq(dto.getStatus() != null, PurchaseOrder::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getPurchaseOrderNo()), PurchaseOrder::getPurchaseOrderNo, dto.getPurchaseOrderNo())
                .orderByDesc(PurchaseOrder::getCreateTime);

        Page<PurchaseOrder> page = purchaseOrderMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<PurchaseOrderVo> vos = toVoList(page.getRecords());

        Page<PurchaseOrderVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public PurchaseOrderVo detailPurchaseOrder(Long id) {
        PurchaseOrder order = getOrderOrThrow(id);
        return toVoList(List.of(order)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrder(Long id, PurchaseOrderUpdateDto dto) {
        PurchaseOrder order = getOrderOrThrow(id);
        Optional.ofNullable(dto.getSupplierId()).ifPresent(order::setSupplierId);
        Optional.ofNullable(dto.getUnitPrice()).ifPresent(order::setUnitPrice);
        Optional.ofNullable(dto.getPlannedQuantity()).ifPresent(order::setPlannedQuantity);
        Optional.ofNullable(dto.getExpectedDeliveryDate()).ifPresent(order::setExpectedDeliveryDate);
        Optional.ofNullable(dto.getUnitPrice())
                .flatMap(up -> Optional.ofNullable(dto.getPlannedQuantity()).map(up::multiply))
                .ifPresent(total -> order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP)));
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException(400, "仅草稿状态采购订单可编辑");
        }
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePurchaseOrder(Long id) {
        PurchaseOrder order = getOrderOrThrow(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException(400, "采购订单状态不为草稿，无法审批");
        }
        require(order.getSupplierId(), "供应商未填写，无法审批");
        require(order.getUnitPrice(), "单价未填写，无法审批");
        require(order.getExpectedDeliveryDate(), "预计交货日期未填写，无法审批");
        require(order.getPlannedQuantity(), "计划数量未填写，无法审批");
        if (order.getUnitPrice() != null && order.getPlannedQuantity() != null) {
            order.setTotalAmount(
                    order.getUnitPrice()
                            .multiply(order.getPlannedQuantity())
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }
        order.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderMapper.updateById(order);

        // 审批通过后自动生成采购入库单（草稿，仓库等信息待实际收货时补充）
        createInStockForOrder(order);
    }

    /**
     * 采购订单审批通过后自动生成采购入库单。
     * 只负责组装数据，创建逻辑（校验、单号生成、状态/类型默认值、落库）复用 PurchaseInStockService。
     *
     * @param order 已审批的采购订单
     */
    private void createInStockForOrder(PurchaseOrder order) {
        PurchaseInStockAddDto dto = new PurchaseInStockAddDto();
        dto.setPurchaseOrderId(order.getId());
        dto.setSupplierId(order.getSupplierId());
        dto.setMaterialId(order.getMaterialId());
        dto.setInQuantity(order.getPlannedQuantity());
        dto.setUnitPrice(order.getUnitPrice());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDeliveryDate(order.getExpectedDeliveryDate());
        purchaseInStockService.addPurchaseInStock(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipPurchaseOrder(Long id) {
        transition(id, PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.SHIPPED,
                "采购订单状态不为已审批，无法发货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receivePurchaseOrder(Long id) {
        transition(id, PurchaseOrderStatus.SHIPPED, PurchaseOrderStatus.RECEIVED,
                "采购订单状态不为已发货，无法收货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePurchaseOrder(Long id) {
        PurchaseOrder order = getOrderOrThrow(id);
        if (order.getStatus() == PurchaseOrderStatus.CLOSED) {
            return;
        }
        if (order.getStatus() != PurchaseOrderStatus.RECEIVED) {
            throw new BusinessException(400, "仅已收货采购订单可关闭");
        }
        order.setStatus(PurchaseOrderStatus.CLOSED);
        purchaseOrderMapper.updateById(order);
    }
}
