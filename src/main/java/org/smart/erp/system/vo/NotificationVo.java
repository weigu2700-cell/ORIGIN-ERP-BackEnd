package org.smart.erp.system.vo;

import lombok.Data;
import org.smart.erp.system.Enum.NotificationType;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
public class NotificationVo {

    private Long id;

    private Long userId;

    private NotificationType type;

    private String title;

    private String content;

    private Long businessId;

    private String businessNo;

    private String businessType;

    private Boolean isRead;

    private LocalDateTime readTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
