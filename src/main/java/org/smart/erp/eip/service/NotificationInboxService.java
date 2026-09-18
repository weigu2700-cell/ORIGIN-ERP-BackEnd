package org.smart.erp.eip.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.eip.dto.NotificationPageDto;
import org.smart.erp.eip.vo.NotificationVo;

public interface NotificationInboxService {
    Page<NotificationVo> page(NotificationPageDto dto);

    NotificationVo get(Long notificationId);

    Long unreadCount();

    void markAsRead(Long notificationId);

    void markAllAsRead();
}
