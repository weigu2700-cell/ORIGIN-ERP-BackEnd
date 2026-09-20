package org.smart.erp.eip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.eip.dto.NotificationTemplateDTO;
import org.smart.erp.eip.service.NotificationTemplateService;
import org.smart.erp.system.Enum.Status;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eip/notification-templates")
@Tag(name = "通知模板", description = "通知模板")
public class NotificationTemplateController {

	private final NotificationTemplateService service;

	public NotificationTemplateController(NotificationTemplateService service) {
		this.service = service;
	}

	@GetMapping
	public Result<List<NotificationTemplateDTO>> list(@RequestParam(required = false) String keyword) {
		return Result.success(service.list(keyword));
	}

	@GetMapping("/{id}")
	public Result<NotificationTemplateDTO> get(@PathVariable Long id) {
		return Result.success(service.get(id));
	}

	@PostMapping
	public Result<NotificationTemplateDTO> create(@RequestBody @Valid NotificationTemplateDTO dto) {
		return Result.success(service.save(dto));
	}

	@PutMapping("/{id}")
	public Result<NotificationTemplateDTO> update(@PathVariable Long id,
			@RequestBody @Valid NotificationTemplateDTO dto) {
		return Result.success(service.update(id, dto));
	}

	@PutMapping("/{id}/status")
	public Result<Void> status(@PathVariable Long id, @RequestParam Status status) {
		service.updateStatus(id, status);
		return Result.success();
	}

	@DeleteMapping("/{id}")
	public Result<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return Result.success();
	}

}
