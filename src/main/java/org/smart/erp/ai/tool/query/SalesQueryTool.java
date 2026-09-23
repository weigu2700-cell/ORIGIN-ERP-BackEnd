package org.smart.erp.ai.tool.query;

import org.smart.erp.ai.tool.ToolExecutionSupport;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.result.PageToolResult;
import org.smart.erp.sales.dto.salesDeliveryDto.SalesDeliveryPageDto;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderPageDto;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesQueryTool {

    private final SalesOrderService salesOrderService;
    private final SalesDeliveryService salesDeliveryService;

    @Tool(name = "query_sales_orders", description = "分页查询销售订单。可按客户ID、销售订单号和订单状态筛选，返回订单列表及分页信息。")
    public PageToolResult<SalesOrderVo> querySalesOrders(
            @ToolParam(required = false, description = "客户ID") Long customerId,
            @ToolParam(required = false, description = "销售订单号") String orderNo,
            @ToolParam(required = false, description = "订单状态：DRAFT(0 草稿)、CONFIRMED(1 已确认)、COMPLETED(2 已完成)、CANCELLED(3 已取消)") SalesOrderStatus status,
            @ToolParam(required = false, description = "页号，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        int pn = ToolExecutionSupport.normalizePage(pageNum);
        int ps = ToolExecutionSupport.normalizePageSize(pageSize);
        SalesOrderPageDto dto = new SalesOrderPageDto();
        dto.setCustomerId(customerId);
        dto.setOrderNo(orderNo);
        dto.setStatus(status);
        dto.setPageNum(pn);
        dto.setPageSize(ps);
        Page<SalesOrderVo> result = ToolExecutionSupport.withSecurityContext(
                toolContext, () -> salesOrderService.pageSalesOrderVoByPage(dto));
        return PageToolResult.from(result);
    }

    @Tool(name = "query_sales_deliveries", description = "分页查询销售出库单。可按销售订单ID、客户ID、出库单号和出库状态筛选，返回出库单列表及分页信息。")
    public PageToolResult<SalesDeliveryVo> querySalesDeliveries(
            @ToolParam(required = false, description = "销售订单ID") Long salesOrderId,
            @ToolParam(required = false, description = "客户ID") Long customerId,
            @ToolParam(required = false, description = "销售出库单号") String deliveryNo,
            @ToolParam(required = false, description = "出库状态：DRAFT(0 草稿)、CONFIRMED(1 已确认)、COMPLETED(3 已完成)、CANCELLED(2 已取消)") SalesDeliveryStatus status,
            @ToolParam(required = false, description = "页号，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        int pn = ToolExecutionSupport.normalizePage(pageNum);
        int ps = ToolExecutionSupport.normalizePageSize(pageSize);
        SalesDeliveryPageDto dto = new SalesDeliveryPageDto();
        dto.setSalesOrderId(salesOrderId);
        dto.setCustomerId(customerId);
        dto.setDeliveryNo(deliveryNo);
        dto.setStatus(status);
        dto.setPageNum(pn);
        dto.setPageSize(ps);
        Page<SalesDeliveryVo> result = ToolExecutionSupport.withSecurityContext(
                toolContext, () -> salesDeliveryService.getPageSalesDeliveryVo(dto));
        return PageToolResult.from(result);
    }

}
