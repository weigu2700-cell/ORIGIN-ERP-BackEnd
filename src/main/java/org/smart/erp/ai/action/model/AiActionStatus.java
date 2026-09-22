package org.smart.erp.ai.action.model;

import lombok.Getter;

@Getter
public enum AiActionStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED,
    EXPIRED
}
