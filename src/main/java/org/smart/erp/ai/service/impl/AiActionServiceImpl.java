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
import java.util.Objects;
import java.util.UUID;

@Service
public class AiActionServiceImpl implements AiActionService {

    private final PurchaseOrderService purchaseOrderService;
    private final OperationString operationString;
    private static final String AI_PENDING_ACTION_KEY = "erp:ai:action:";
    private static final String AI_ACTION_EXECUTION_LOCK_KEY = "erp:ai:action:executing:";
    private static final Duration ACTION_TTL = Duration.ofMinutes(10);
    private static final Duration EXECUTION_LOCK_TTL = Duration.ofMinutes(1);

    public AiActionServiceImpl(
            PurchaseOrderService purchaseOrderService,
            OperationString operationString
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.operationString = operationString;
    }

    @Override
    public AiActionProposal prepareApprovePurchaseOrder4Ai(String purchaseOrderNo) {
        if (purchaseOrderNo == null || purchaseOrderNo.isBlank()) {
            throw new BusinessException(400, "采购订单编号不能为空");
        }
        PurchaseOrder purchaseOrder = purchaseOrderService.getOne(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getPurchaseOrderNo, purchaseOrderNo.trim())
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
                "确认后将审批采购订单 " + purchaseOrder.getPurchaseOrderNo()
                        + "，并自动生成采购入库单。"
        );
    }

    @Override
    public List<AiPendingActionResult> createPendingAction(
            List<AiActionProposal> proposals,
            Long userId,
            Long conversationId
    ) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plus(ACTION_TTL);

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
                    operationString.set(redisKey, aiPendingAction, ACTION_TTL);

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

    @Override
    public void executePendingAction(String token, Long userId) {
        validateToken(token);
        if (userId == null) {
            throw new BusinessException(401, "未认证或登录已失效");
        }

        String redisKey = AI_PENDING_ACTION_KEY + token;
        String lockKey = AI_ACTION_EXECUTION_LOCK_KEY + token;
        boolean locked = operationString.setIfAbsent(lockKey, userId, EXECUTION_LOCK_TTL);
        if (!locked) {
            throw new BusinessException(409, "该操作正在执行，请勿重复确认");
        }

        try {
            AiPendingAction pendingAction = operationString.get(redisKey);
            if (pendingAction == null) {
                throw new BusinessException(410, "待确认操作不存在或已过期，请重新发起");
            }
            if (!Objects.equals(pendingAction.userId(), userId)) {
                throw new BusinessException(403, "无权执行该待确认操作");
            }
            if (pendingAction.expireTime() == null
                    || !pendingAction.expireTime().isAfter(LocalDateTime.now())) {
                operationString.delete(redisKey);
                throw new BusinessException(410, "待确认操作已过期，请重新发起");
            }
            if (pendingAction.status() != AiActionStatus.PENDING) {
                throw new BusinessException(409, "该操作已处理，请勿重复确认");
            }

            execute(pendingAction, redisKey);
            // 只在业务执行成功后消费 token；瞬时故障时用户仍可在有效期内重试。
            operationString.delete(redisKey);
        } finally {
            operationString.delete(lockKey);
        }
    }

    private void execute(AiPendingAction pendingAction, String redisKey) {
        switch (pendingAction.actionType()) {
            case APPROVE_PURCHASE_ORDER -> {
                PurchaseOrder current = purchaseOrderService.getById(pendingAction.bizId());
                if (current == null || !Objects.equals(current.getPurchaseOrderNo(), pendingAction.bizNo())) {
                    operationString.delete(redisKey);
                    throw new BusinessException(404, "采购订单不存在，待确认操作已失效");
                }
                if (!Objects.equals(current.getVersion(), pendingAction.version())) {
                    operationString.delete(redisKey);
                    throw new BusinessException(409, "采购订单已被修改，请重新发起审批");
                }
                // 调用领域服务而不是直接改状态，复用权限、状态、入库单与通知逻辑。
                purchaseOrderService.approvePurchaseOrder(pendingAction.bizId());
            }
        }
    }

    private static void validateToken(String token) {
        if (token == null || !token.matches("^[0-9a-f]{32}$")) {
            throw new BusinessException(400, "无效的待确认操作 token");
        }
    }
}
