package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.ai.enums.AiActionStatus;
import org.smart.erp.ai.model.AiPendingAction;
import org.smart.erp.ai.result.AiPendingActionResult;
import org.smart.erp.ai.service.AiActionService;
import org.smart.erp.ai.model.AiActionProposal;
import org.smart.erp.ai.enums.AiActionType;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiActionServiceImpl implements AiActionService {

    private final PurchaseOrderService purchaseOrderService;
    private final OperationString operationString;
    private final String AI_PENDING_ACTION_KEY = "erp:ai:action:";

    public AiActionServiceImpl(
            PurchaseOrderService purchaseOrderService,
            OperationString operationString
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.operationString = operationString;
    }

    @Override
    public AiActionProposal prepareApprovePurchaseOrder4Ai(String purchaseOrderNo) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getOne(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getPurchaseOrderNo,purchaseOrderNo)
        );
        if (purchaseOrder == null) {
            throw new BusinessException(404, "采购订单不存在");
        }
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

    @Override
    public List<AiPendingActionResult> createPendingAction(
            List<AiActionProposal> proposals,
            Long userId,
            Long conversationId
    ) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(10);

        return proposals.stream()
                .map(proposal -> {

                    String token = UUID.randomUUID()
                            .toString()
                            .replace("-", "");

                    AiPendingAction aiPendingAction =
                            new AiPendingAction(
                                    token,
                                    userId,
                                    conversationId,
                                    proposal.actionType(),
                                    proposal.bizId(),
                                    proposal.bizNo(),
                                    proposal.version(),
                                    proposal.title(),
                                    proposal.description(),
                                    AiActionStatus.PENDING,
                                    now,
                                    expireTime
                            );

                    String redisKey = AI_PENDING_ACTION_KEY + token;
                    operationString.set(redisKey, aiPendingAction, Duration.ofMinutes(10));

                    return new AiPendingActionResult(
                            token,
                            proposal.actionType(),
                            proposal.bizNo(),
                            proposal.title(),
                            proposal.description(),
                            expireTime
                    );
                }).toList();
    }
}
