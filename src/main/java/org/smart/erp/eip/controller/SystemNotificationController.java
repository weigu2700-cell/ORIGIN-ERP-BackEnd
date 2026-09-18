package org.smart.erp.eip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.smart.erp.eip.service.SystemNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eip/notifications")
@Tag(name = "系统通知", description = "系统通知")
public class SystemNotificationController {
    private final SystemNotificationService service;

    public SystemNotificationController(SystemNotificationService service) {
        this.service = service;
    }

    @PostMapping("/publish")
    @Operation(summary = "发布系统通知")
    @PreAuthorize("hasAuthority('eip:notification:publish')")
    public Result<String> publish(@RequestBody @Valid SystemNotificationPublishDTO dto) {
        return Result.success(service.publish(dto));
    }
}
