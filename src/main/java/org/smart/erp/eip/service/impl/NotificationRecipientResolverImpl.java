package org.smart.erp.eip.service.impl;

import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.port.RecipientDirectory;
import org.smart.erp.eip.service.NotificationRecipientResolver;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class NotificationRecipientResolverImpl implements NotificationRecipientResolver {

	private final RecipientDirectory directory;

	public NotificationRecipientResolverImpl(RecipientDirectory directory) {
		this.directory = directory;
	}

	@Override
	public Set<Long> resolve(RecipientSelectorDTO selector) {
		if (selector != null && selector.isAllActiveUsers()
				&& (hasValues(selector.getUserIds()) || hasValues(selector.getRoleCodes())
						|| hasValues(selector.getPermissionCodes()) || hasValues(selector.getDepartmentIds())
						|| selector.isIncludeAdministrators())) {
			throw new BusinessException(422, "全部启用用户不能与其他收件人条件同时使用");
		}
		return directory.resolve(selector);
	}

	private boolean hasValues(Set<?> values) {
		return values != null && !values.isEmpty();
	}

}