package org.smart.erp.eip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.service.NotificationRecipientOptionsService;
import org.smart.erp.eip.vo.RecipientOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eip")
@Tag(name = "通知收件人", description = "通知收件人")
public class NotificationRecipientController {

	private final NotificationRecipientOptionsService service;

	public NotificationRecipientController(NotificationRecipientOptionsService service) {
		this.service = service;
	}

	@GetMapping("/notification-recipient-options")
	@Operation(summary = "查询可选通知收件人")
	public Result<List<RecipientOptionVO>> options(@RequestParam RecipientSelectorType type,
			@RequestParam(required = false) String keyword) {
		return Result.success(service.options(type, keyword));
	}

}
