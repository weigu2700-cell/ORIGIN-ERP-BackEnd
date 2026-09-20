package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SystemNotificationService {

	@PreAuthorize("hasAuthority('eip:notification:publish')")
	String publish(SystemNotificationPublishDTO dto);

}
