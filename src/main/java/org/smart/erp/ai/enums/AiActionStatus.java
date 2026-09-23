package org.smart.erp.ai.enums;

import lombok.Getter;

@Getter
public enum AiActionStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED,
    EXPIRED
}
