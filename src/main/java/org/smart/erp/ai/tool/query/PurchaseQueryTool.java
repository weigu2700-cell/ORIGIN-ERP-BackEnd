package org.smart.erp.ai.tool.query;

import org.smart.erp.ai.tool.ToolExecutionSupport;

import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.result.PageToolResult;
import org.smart.erp.purchase.dto.PurchaseDemandPageDto;
import org.smart.erp.purchase.dto.PurchaseOrderPageDto;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseDemandVo;
import org.smart.erp.purchase.vo.PurchaseOrderVo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseQueryTool {

    private final PurchaseDemandService purchaseDemandService;
    private final PurchaseOrderService purchaseOrderService;

    @Tool(name = "query_purchase_demands", description = "分页查询采购需求。可按物料、来源类型、来源单号和需求状态筛选；只读操作，返回采购需求分页结果。")
    public PageToolResult<PurchaseDemandVo> queryPurchaseDemands(
            @ToolParam(required = false, description = "物料ID") Long materialId,
            @ToolParam(required = false, description = "需求来源类型枚举：PRODUCTION_ORDER（0，生产订单）或 OTHER（1，其他）") PurchaseDemandSourceType sourceType,
            @ToolParam(required = false, description = "来源业务单号，例如生产订单号") String sourceNo,
            @ToolParam(required = false, description = "采购需求状态枚举：DRAFT（0，草稿）、APPROVED（1，已审批）或 CLOSED（2，已关闭）") PurchaseDemandStatus status,
            @ToolParam(required = false, description = "页码，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        PurchaseDemandPageDto dto = new PurchaseDemandPageDto();
        dto.setMaterialId(materialId);
        dto.setSourceType(sourceType);
        dto.setSourceNo(sourceNo);
        dto.setStatus(status);
        setPage(dto, pageNum, pageSize);
        return ToolExecutionSupport.withSecurityContext(toolContext,
                () -> PageToolResult.from(purchaseDemandService.pagePurchaseDemand(dto)));
    }

    @Tool(name = "query_purchase_orders", description = "分页查询采购订单。可按采购订单号、物料、供应商和订单状态筛选；只读操作，返回采购订单分页结果。")
    public PageToolResult<PurchaseOrderVo> queryPurchaseOrders(
            @ToolParam(required = false, description = "采购订单号，支持按单号查询") String purchaseOrderNo,
            @ToolParam(required = false, description = "物料ID") Long materialId,
            @ToolParam(required = false, description = "供应商ID") Long supplierId,
            @ToolParam(
                    required = false,
                    description = "采购订单状态枚举：DRAFT（0，草稿）、APPROVED（1，已审批）、SHIPPED（2，已发货）、RECEIVED（3，已收货）或 CLOSED（4，已关闭）"
            ) PurchaseOrderStatus status,
            @ToolParam(required = false, description = "页码，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        PurchaseOrderPageDto dto = new PurchaseOrderPageDto();
        dto.setPurchaseOrderNo(purchaseOrderNo);
        dto.setMaterialId(materialId);
        dto.setSupplierId(supplierId);
        dto.setStatus(status);
        setPage(dto, pageNum, pageSize);
        return ToolExecutionSupport.withSecurityContext(toolContext,
                () -> PageToolResult.from(purchaseOrderService.pagePurchaseOrder(dto)));
    }

    private static void setPage(PurchaseDemandPageDto dto, Integer pageNum, Integer pageSize) {
        dto.setPageNum(ToolExecutionSupport.normalizePage(pageNum));
        dto.setPageSize(ToolExecutionSupport.normalizePageSize(pageSize));
    }

    private static void setPage(PurchaseOrderPageDto dto, Integer pageNum, Integer pageSize) {
        dto.setPageNum(ToolExecutionSupport.normalizePage(pageNum));
        dto.setPageSize(ToolExecutionSupport.normalizePageSize(pageSize));
    }

}
