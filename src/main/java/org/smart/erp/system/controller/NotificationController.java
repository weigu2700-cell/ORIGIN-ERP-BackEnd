package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.NotificationPageDto;
import org.smart.erp.system.service.NotificationService;
import org.smart.erp.system.vo.NotificationVo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "sys/notification")
@Tag(name = "通知", description = "通知")
public class NotificationController {

    private final NotificationService notificationService;

    private NotificationController (NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(description = "分页查询通知")
    public Result<Page<NotificationVo>> add(
            @Parameter(name = "dto", description = "分页查询通知") NotificationPageDto dto) {
        return Result.success(notificationService.pageNotification(dto));
    }

    @GetMapping("/{id}")
    @Operation(description = "查询通知详情")
    public Result<NotificationVo> getDetail(
            @PathVariable("id") @Parameter(name = "id", description = "通知id") Long notificationId) {
        return Result.success(notificationService.getNotification(notificationId));
    }

    @GetMapping("/unread/count")
    @Operation(description = "获取未读通知数量")
    public Result<Long> getUnReadCount() {
        return Result.success(notificationService.getUnReadCount());
    }

    @PutMapping("{id}/readed")
    @Operation(description = "标记为已读")
    public Result<Void> markRead(
            @PathVariable("id") @Parameter(name = "id", description = "通知id") Long notificationId) {
        notificationService.markAsRead(notificationId);
        return Result.success();
    }

    @PutMapping("/all/readed")
    @Operation(description = "标记所有通知为已读")
    public Result<Void> markAllRead(){
        notificationService.markAllAsRead();
        return Result.success();
    }
}
