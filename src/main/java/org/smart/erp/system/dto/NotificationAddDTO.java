package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.NotificationType;

import java.util.List;

@Data
public class NotificationAddDTO {

    private Long userId;

    private NotificationType type;

    private String title;

    private String content;

    private String businessType;

    private Long businessId;

    private String businessNo;
}