package org.smart.erp.eip.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 通知在收件箱中展示的业务类型。数值保持与原 sys_notification 约定兼容。 */
@Getter
public enum NotificationType {

	SYSTEM(0, "系统消息"), BUSINESS(1, "业务消息"), WARNING(2, "预警消息"), TASK(3, "任务消息"), CUSTOM(4, "自定义消息");

	@EnumValue
	@JsonValue
	private final Integer code;

	private final String desc;

	NotificationType(Integer code, String desc) {
		this.code = code;
		this.desc = desc;
	}

}