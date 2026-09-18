package org.smart.erp.eip.service;

import org.smart.erp.eip.event.NotificationPublishEvent;

import java.util.Set;

/** 发布批次幂等落库及收件箱批量写入。 */
public interface NotificationPersistenceService {
    void persist(NotificationPublishEvent event, Set<Long> recipientIds);
}
