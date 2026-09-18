package org.smart.erp.eip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/** 收件人组合传输对象，使调用方无需耦合系统表即可组装收件人。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientSelectorDTO {

	@Builder.Default
	private Set<Long> userIds = new LinkedHashSet<>();

	@Builder.Default
	private Set<String> roleCodes = new LinkedHashSet<>();

	@Builder.Default
	private Set<String> permissionCodes = new LinkedHashSet<>();

	@Builder.Default
	private Set<Long> departmentIds = new LinkedHashSet<>();

	private boolean allActiveUsers;

	private boolean includeChildDepartments;

	private boolean includeAdministrators;

	public static RecipientSelectorDTO permissions(Set<String> permissionCodes, boolean includeAdministrators) {
		RecipientSelectorDTO selector = new RecipientSelectorDTO();
		selector
			.setPermissionCodes(permissionCodes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissionCodes));
		selector.setIncludeAdministrators(includeAdministrators);
		return selector;
	}

	public static RecipientSelectorDTO users(Set<Long> userIds) {
		RecipientSelectorDTO selector = new RecipientSelectorDTO();
		selector.setUserIds(userIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(userIds));
		return selector;
	}

}