package org.smart.erp.eip.dto;

import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.smart.erp.eip.enums.NotificationType;

/** 业务通知与系统通知共有的发布字段，作为两类发布 DTO 的公共基类。 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractNotificationPublishDTO {

	private String requestId;

	private NotificationType type;

	private String title;

	private String content;

	@Valid
	@Builder.Default
	private RecipientSelectorDTO recipients = new RecipientSelectorDTO();

}
