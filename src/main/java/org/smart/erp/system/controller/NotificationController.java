package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.NotificationPageDto;
import org.smart.erp.system.service.NotificationService;
import org.smart.erp.system.vo.NotificationVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "sys/notification")
public class NotificationController {

    private final NotificationService notificationService;

    private NotificationController (NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Page<NotificationVo>> add(NotificationPageDto dto) {
        return Result.success(notificationService.pageNotification(dto));
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnReadCount(Long userId) {
        return Result.success(notificationService.getUnReadCount(userId));
    }

    @PutMapping("/readed")
    public Result<Void> markRead(Long userId, Long notificationId) {
        notificationService.markAsRead(userId, notificationId);
        return Result.success();
    }

    @PutMapping("/all/readed")
    public Result<Void> markAllRead(Long userId){
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}

