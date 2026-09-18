package org.smart.erp.eip.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.eip.dto.NotificationPageDto;
import org.smart.erp.eip.service.NotificationInboxService;
import org.smart.erp.eip.vo.NotificationVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sys/notification")
@Tag(name = "通知", description = "通知")
public class NotificationController {
    private final NotificationInboxService inboxService;

    public NotificationController(NotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    @Operation(description = "分页查询通知")
    public Result<Page<NotificationVo>> page(
            @Parameter(name = "dto", description = "分页查询通知") NotificationPageDto dto) {
        return Result.success(inboxService.page(dto));
    }

    @GetMapping("/{id}")
    @Operation(description = "查询通知详情")
    public Result<NotificationVo> getDetail(
            @PathVariable("id") @Parameter(name = "id", description = "通知id") Long notificationId) {
        return Result.success(inboxService.get(notificationId));
    }

    @GetMapping("/unread/count")
    @Operation(description = "获取未读通知数量")
    public Result<Long> getUnReadCount() {
        return Result.success(inboxService.unreadCount());
    }

    @PutMapping("{id}/readed")
    @Operation(description = "标记为已读")
    public Result<Void> markRead(
            @PathVariable("id") @Parameter(name = "id", description = "通知id") Long notificationId) {
        inboxService.markAsRead(notificationId);
        return Result.success();
    }

    @PutMapping("/all/readed")
    @Operation(description = "标记所有通知为已读")
    public Result<Void> markAllRead() {
        inboxService.markAllAsRead();
        return Result.success();
    }
}
