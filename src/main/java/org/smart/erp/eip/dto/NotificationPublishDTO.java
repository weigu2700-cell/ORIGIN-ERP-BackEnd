package org.smart.erp.eip.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.enums.NotificationType;

/** 业务模块发布通知的参数对象。 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Jacksonized
public class NotificationPublishDTO extends AbstractNotificationPublishDTO {

	private NotificationSourceType sourceType;

	private NotificationBusinessRefDTO business;

	public static NotificationPublishDTO business(
			NotificationType type,
			String title,
			String content,
			NotificationBusinessRefDTO business,
			RecipientSelectorDTO recipients
	) {
		if (business == null || business.getBusinessType() == null || business.getBusinessId() == null) {
			throw new IllegalArgumentException("业务通知必须提供业务类型和业务ID");
		}
		return NotificationPublishDTO.builder()
			.requestId("BUSINESS:" + business.getBusinessType() + ":" + business.getBusinessId())
			.sourceType(NotificationSourceType.BUSINESS)
			.type(type)
			.title(title)
			.content(content)
			.business(business)
			.recipients(recipients)
			.build();
	}

}
