package org.smart.erp.ai.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.tool.result.PageToolResult;
import org.smart.erp.production.dto.ProductionDemandPageDto;
import org.smart.erp.production.dto.ProductionOrderPageDto;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.vo.ProductionDemandVo;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionQueryTool {

    private final ProductionOrderService productionOrderService;
    private final ProductionDemandService productionDemandService;

    @Tool(name = "query_production_orders", description = "分页查询生产订单。可按生产订单号、生产需求号、物料ID和生产订单状态筛选，返回生产订单列表及分页信息。")
    public PageToolResult<ProductionOrderVo> queryProductionOrders(
            @ToolParam(required = false, description = "生产订单号") String productionOrderNo,
            @ToolParam(required = false, description = "生产需求号") String productionDemandNo,
            @ToolParam(required = false, description = "物料ID；按生产订单分页接口约定使用字符串") String materialId,
            @ToolParam(required = false, description = "生产订单状态：DRAFT(0 草稿)、RELEASED(1 已下达)、IN_PROGRESS(2 生产中)、COMPLETED(3 已完成)、CANCELLED(4 已取消)") ProductionOrderStatus status,
            @ToolParam(required = false, description = "页号，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        int pn = ToolExecutionSupport.normalizePage(pageNum);
        int ps = ToolExecutionSupport.normalizePageSize(pageSize);
        ProductionOrderPageDto dto = new ProductionOrderPageDto();
        dto.setProductionOrderNo(productionOrderNo);
        dto.setProductionDemandNo(productionDemandNo);
        dto.setMaterialId(materialId);
        dto.setStatus(status);
        dto.setPageNum(pn);
        dto.setPageSize(ps);
        Page<ProductionOrderVo> result = ToolExecutionSupport.withSecurityContext(
                toolContext, () -> productionOrderService.pageProductionOrder(dto));
        return PageToolResult.from(result);
    }

    @Tool(name = "query_production_demands", description = "分页查询生产需求。可按需求号、物料ID、需求来源类型和生产需求状态筛选，返回生产需求列表及分页信息。")
    public PageToolResult<ProductionDemandVo> queryProductionDemands(
            @ToolParam(required = false, description = "生产需求号") String demandNo,
            @ToolParam(required = false, description = "物料ID") Long materialId,
            @ToolParam(required = false, description = "需求来源类型：SALES_ORDER(0 销售订单)") ProductionSourceType sourceType,
            @ToolParam(required = false, description = "生产需求状态：PENDING(0 待生产)、PLANNED(1 已计划)、CANCELLED(2 已取消)、COMPLETED(3 已完成)") ProductionStatus status,
            @ToolParam(required = false, description = "页号，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        int pn = ToolExecutionSupport.normalizePage(pageNum);
        int ps = ToolExecutionSupport.normalizePageSize(pageSize);
        ProductionDemandPageDto dto = new ProductionDemandPageDto();
        dto.setDemandNo(demandNo);
        dto.setMaterialId(materialId);
        dto.setSourceType(sourceType);
        dto.setStatus(status);
        dto.setPageNum(pn);
        dto.setPageSize(ps);
        Page<ProductionDemandVo> result = ToolExecutionSupport.withSecurityContext(
                toolContext, () -> productionDemandService.pageProductionDemand(dto));
        return PageToolResult.from(result);
    }

}
