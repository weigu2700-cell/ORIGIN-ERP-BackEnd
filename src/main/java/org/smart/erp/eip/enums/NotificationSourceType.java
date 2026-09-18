package org.smart.erp.eip.enums;

import lombok.Getter;

/** 通知发布来源，用于区分系统预置通知和业务模块通知。 */
@Getter
public enum NotificationSourceType {

	SYSTEM, BUSINESS

}