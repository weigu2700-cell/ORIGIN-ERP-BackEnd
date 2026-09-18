package org.smart.erp.eip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.Data;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.system.Enum.Status;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationTemplateDTO {

	private Long id;

	@NotBlank
	private String code;

	@NotBlank
	private String name;

	@NotBlank
	private String titleTemplate;

	@NotBlank
	private String contentTemplate;

	private NotificationType notificationType = NotificationType.SYSTEM;

	private Status status = Status.ENABLE;

	private String remark;

	@Valid
	private List<NotificationTemplateRecipientDTO> recipients = new ArrayList<>();

}