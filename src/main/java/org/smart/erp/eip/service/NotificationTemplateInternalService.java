package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.NotificationTemplateDTO;

/** Internal template lookup used by notification orchestration. */
public interface NotificationTemplateInternalService {
	NotificationTemplateDTO getInternally(Long id);
}
