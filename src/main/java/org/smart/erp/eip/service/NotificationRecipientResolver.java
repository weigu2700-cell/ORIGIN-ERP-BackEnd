package org.smart.erp.eip.service;

import org.smart.erp.eip.dto.RecipientSelectorDTO;

import java.util.Set;

/** 将发布请求中的收件人选择器解析为最终用户 ID。 */
public interface NotificationRecipientResolver {

	Set<Long> resolve(RecipientSelectorDTO selector);

}