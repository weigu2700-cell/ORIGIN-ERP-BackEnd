package org.smart.erp.ai.action.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.ai.action.service.AiActionService;
import org.smart.erp.ai.action.model.AiActionProposal;
import org.smart.erp.ai.action.model.AiActionType;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

public class AiActionServiceImpl implements AiActionService {

    private final PurchaseOrderService purchaseOrderService;

    public AiActionServiceImpl(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @Override
    public AiActionProposal prepareApprovePurchaseOrder4Ai(String purchaseOrderNo) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getOne(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getPurchaseOrderNo,purchaseOrderNo)
        );
        if (!purchaseOrderService.checkPurchaseOrder(purchaseOrder.getId())) {
            throw new BusinessException(400, "采购订单信息不完整，暂时无法审核");
        }

        return new AiActionProposal(
                AiActionType.APPROVE_PURCHASE_ORDER,
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseOrderNo(),
                purchaseOrder.getVersion(),
                "审批采购订单 " + purchaseOrder.getPurchaseOrderNo(),
                "采购订单 " + purchaseOrder.getPurchaseOrderNo() + " 已准备好审批"
        );
    }
}
