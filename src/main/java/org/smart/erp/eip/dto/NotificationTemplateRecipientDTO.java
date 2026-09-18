package org.smart.erp.eip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.smart.erp.eip.enums.RecipientSelectorType;

@Data
public class NotificationTemplateRecipientDTO {

	@NotNull
	private RecipientSelectorType selectorType;

	@NotBlank
	private String selectorValue;

	private boolean includeChildren;

}