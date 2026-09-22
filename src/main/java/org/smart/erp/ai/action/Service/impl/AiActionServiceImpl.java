package org.smart.erp.ai.action.service.impl;

import org.smart.erp.ai.action.service.AiActionService;
import org.smart.erp.ai.action.model.AiActionProposal;
import org.smart.erp.ai.action.model.AiActionType;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

public class AiActionServiceImpl implements AiActionService {

    private final PurchaseOrderService purchaseOrderService;

    public AiActionServiceImpl(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @Override
    public AiActionProposal prepareApprovePurchaseOrder4Ai(Long id) {
        PurchaseOrderVo purchaseOrder = purchaseOrderService.detailPurchaseOrder(id);
        boolean ready = purchaseOrderService.checkPurchaseOrder(id);
        return new AiActionProposal(
                AiActionType.APPROVE_PURCHASE_ORDER,
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseOrderNo(),
                purchaseOrder.getVersion(),
                "审批采购订单 " + purchaseOrder.getPurchaseOrderNo(),
                ready ? "采购订单信息完整，可提交审批" : "采购订单存在未填写的必填项，审批前请先补全"
        );
    }
}
