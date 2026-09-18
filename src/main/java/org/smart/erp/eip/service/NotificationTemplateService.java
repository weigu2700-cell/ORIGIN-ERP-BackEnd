package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.NotificationTemplateDTO;
import org.smart.erp.system.Enum.Status;

import java.util.List;

public interface NotificationTemplateService {
    List<NotificationTemplateDTO> list(String keyword);
    NotificationTemplateDTO get(Long id);
    NotificationTemplateDTO save(NotificationTemplateDTO dto);
    NotificationTemplateDTO update(Long id, NotificationTemplateDTO dto);
    void updateStatus(Long id, Status status);
    void delete(Long id);
}
