package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.NotificationTemplateDTO;
import org.smart.erp.system.Enum.Status;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NotificationTemplateService {

	@PreAuthorize("hasAuthority('eip:notification:template:list')")
	List<NotificationTemplateDTO> list(String keyword);

	@PreAuthorize("hasAuthority('eip:notification:template:get')")
	NotificationTemplateDTO get(Long id);

	@PreAuthorize("hasAuthority('eip:notification:template:create')")
	NotificationTemplateDTO save(NotificationTemplateDTO dto);

	@PreAuthorize("hasAuthority('eip:notification:template:update')")
	NotificationTemplateDTO update(Long id, NotificationTemplateDTO dto);

	@PreAuthorize("hasAuthority('eip:notification:template:status')")
	void updateStatus(Long id, Status status);

	@PreAuthorize("hasAuthority('eip:notification:template:delete')")
	void delete(Long id);

}
