package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.NotificationAddDTO;
import org.smart.erp.system.dto.NotificationPageDto;
import org.smart.erp.system.entity.Notification;
import org.smart.erp.system.vo.NotificationVo;

public interface NotificationService extends IService<Notification> {

    void addNotification(NotificationAddDTO dto);

    Page<NotificationVo> pageNotification(NotificationPageDto dto);

    Long getUnReadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
