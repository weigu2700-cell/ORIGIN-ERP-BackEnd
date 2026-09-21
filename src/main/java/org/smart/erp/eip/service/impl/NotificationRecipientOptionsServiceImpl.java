package org.smart.erp.eip.service.impl;

import lombok.RequiredArgsConstructor;
import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.port.RecipientDirectory;
import org.smart.erp.eip.service.NotificationRecipientOptionsService;
import org.smart.erp.eip.vo.RecipientOptionVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationRecipientOptionsServiceImpl implements NotificationRecipientOptionsService {

	private final RecipientDirectory directory;

	@Override
	public List<RecipientOptionVO> options(RecipientSelectorType type, String keyword) {
		return directory.options(type, keyword);
	}
}
