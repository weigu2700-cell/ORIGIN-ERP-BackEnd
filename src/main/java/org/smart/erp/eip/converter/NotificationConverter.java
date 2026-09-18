package org.smart.erp.eip.converter;

import org.smart.erp.eip.dto.NotificationBusinessRefDTO;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.entity.Notification;
import org.smart.erp.eip.entity.NotificationPublish;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.vo.NotificationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

	public NotificationPublish toPublish(NotificationPublishDTO dto, String requestId) {
		NotificationPublish result = new NotificationPublish();
		result.setRequestId(requestId);
		result.setSourceType(dto.getSourceType() == null ? NotificationSourceType.BUSINESS : dto.getSourceType());
		result.setType(dto.getType() == null ? NotificationType.SYSTEM : dto.getType());
		result.setTitle(dto.getTitle());
		result.setContent(dto.getContent());
		NotificationBusinessRefDTO business = dto.getBusiness();
		if (business != null) {
			result.setBusinessType(business.getBusinessType());
			result.setBusinessId(business.getBusinessId());
			result.setBusinessNo(business.getBusinessNo());
		}
		return result;
	}

	public Notification toNotification(NotificationPublish publish, Long userId) {
		Notification notification = new Notification();
		notification.setPublishId(publish.getId());
		notification.setUserId(userId);
		notification.setType(publish.getType());
		notification.setTitle(publish.getTitle());
		notification.setContent(publish.getContent());
		notification.setBusinessType(publish.getBusinessType());
		notification.setBusinessId(publish.getBusinessId());
		notification.setBusinessNo(publish.getBusinessNo());
		notification.setIsRead(false);
		return notification;
	}

	public NotificationVo toVo(Notification notification) {
		NotificationVo vo = new NotificationVo();
		BeanUtils.copyProperties(notification, vo);
		return vo;
	}

}