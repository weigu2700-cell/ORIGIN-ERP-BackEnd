package org.smart.erp.eip.service;

import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.vo.RecipientOptionVO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/** EIP 收件人选项的受保护用例入口；目录端口本身保持内部协作用途。 */
public interface NotificationRecipientOptionsService {

	@PreAuthorize("hasAnyAuthority('eip:notification:recipient:list', 'eip:notification:publish')")
	List<RecipientOptionVO> options(RecipientSelectorType type, String keyword);
}
