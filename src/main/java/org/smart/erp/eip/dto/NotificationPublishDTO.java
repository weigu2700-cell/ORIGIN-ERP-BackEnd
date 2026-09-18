package org.smart.erp.eip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.enums.NotificationType;

/** 冻结的 EIP 通知发布边界；业务方只需提交一个参数对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPublishDTO {

	private String requestId;

	private NotificationSourceType sourceType;

	private NotificationType type;

	private String title;

	private String content;

	private NotificationBusinessRefDTO business;

	private RecipientSelectorDTO recipients;

	public static NotificationPublishDTO business(NotificationType type, String title, String content,
			NotificationBusinessRefDTO business, RecipientSelectorDTO recipients) {
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