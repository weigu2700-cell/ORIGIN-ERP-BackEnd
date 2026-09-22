package org.smart.erp.ai.dto.request;

import com.baomidou.mybatisplus.annotation.IEnum;
import org.apache.poi.ss.formula.functions.T;
import org.smart.erp.ai.enums.AiActionStatus;
import org.smart.erp.ai.enums.AiActionType;

import java.time.LocalDateTime;

public record AiPendingAction(
        String token,
        Long conversationId,
        AiActionType type,
        Long bizId,
        String bizNo,
        Integer version,
        String title,
        String description,
        AiActionStatus status,
        LocalDateTime createTime,
        LocalDateTime expireTime
){
}
