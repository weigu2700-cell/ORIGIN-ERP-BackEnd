package org.smart.erp.ai.tool.action.enums;

import lombok.Getter;

@Getter
public enum AiActionStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED,
    EXPIRED
}
