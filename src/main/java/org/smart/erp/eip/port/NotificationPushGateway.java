package org.smart.erp.eip.port;

import org.smart.erp.eip.entity.Notification;

/** 通知领域对实时推送能力的端口。 */
public interface NotificationPushGateway {

	void push(Notification notification);

}