package org.smart.erp.eip.event;

import lombok.Getter;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/** 原业务事务中发布，监听器在事务提交后才执行通知落库。 */
@Getter
public final class NotificationPublishEvent extends ApplicationEvent {

	private final NotificationPublishDTO publish;

	private final String requestId;
	private final String tenantId;

	public NotificationPublishEvent(Object source, NotificationPublishDTO publish, String tenantId) {
		super(source);
		if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("通知缺少企业身份");
		this.tenantId = tenantId;
		this.publish = freeze(publish);
		this.requestId = publish.getRequestId() == null || publish.getRequestId().isBlank()
				? UUID.randomUUID().toString() : publish.getRequestId();
	}

	private static NotificationPublishDTO freeze(NotificationPublishDTO dto) {
		var selector = dto.getRecipients();
		var recipients = selector == null ? null : org.smart.erp.eip.dto.RecipientSelectorDTO.builder()
			.userIds(copy(selector.getUserIds())).roleCodes(copy(selector.getRoleCodes()))
			.permissionCodes(copy(selector.getPermissionCodes())).departmentIds(copy(selector.getDepartmentIds()))
			.allActiveUsers(selector.isAllActiveUsers()).includeChildDepartments(selector.isIncludeChildDepartments())
			.includeAdministrators(selector.isIncludeAdministrators()).build();
		var business = dto.getBusiness();
		var reference = business == null ? null : org.smart.erp.eip.dto.NotificationBusinessRefDTO.of(
				business.getBusinessType(), business.getBusinessId(), business.getBusinessNo());
		return NotificationPublishDTO.builder().requestId(dto.getRequestId()).sourceType(dto.getSourceType())
			.type(dto.getType()).title(dto.getTitle()).content(dto.getContent()).recipients(recipients).business(reference).build();
	}

	private static <T> java.util.Set<T> copy(java.util.Set<T> values) {
		return values == null ? java.util.Set.of() : new java.util.LinkedHashSet<>(values);
	}

}
