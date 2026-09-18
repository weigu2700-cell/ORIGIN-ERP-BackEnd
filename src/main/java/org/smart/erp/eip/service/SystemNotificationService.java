package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.SystemNotificationPublishDTO;

public interface SystemNotificationService {
    String publish(SystemNotificationPublishDTO dto);
}
