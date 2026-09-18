package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.NotificationPublishDTO;

/** 业务模块发布通知的唯一入口。 */
public interface NotificationPublisher {

	void publish(NotificationPublishDTO dto);

}