package org.smart.erp.eip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.eip.dto.NotificationTemplateDTO;
import org.smart.erp.eip.dto.NotificationTemplateRecipientDTO;
import org.smart.erp.eip.entity.NotificationTemplate;
import org.smart.erp.eip.entity.NotificationTemplateRecipient;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.mapper.NotificationTemplateMapper;
import org.smart.erp.eip.mapper.NotificationTemplateRecipientMapper;
import org.smart.erp.eip.service.NotificationTemplateService;
import org.smart.erp.eip.service.NotificationTemplateInternalService;
import org.smart.erp.system.Enum.Status;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService, NotificationTemplateInternalService {

	private final NotificationTemplateMapper templateMapper;

	private final NotificationTemplateRecipientMapper recipientMapper;

	public NotificationTemplateServiceImpl(NotificationTemplateMapper templateMapper,
			NotificationTemplateRecipientMapper recipientMapper) {
		this.templateMapper = templateMapper;
		this.recipientMapper = recipientMapper;
	}

	@Override
	public List<NotificationTemplateDTO> list(String keyword) {
		String value = keyword == null ? "" : keyword.trim();
		return templateMapper
			.selectList(new LambdaQueryWrapper<NotificationTemplate>()
				.and(w -> w.like(NotificationTemplate::getCode, value).or().like(NotificationTemplate::getName, value))
				.orderByDesc(NotificationTemplate::getUpdateTime))
			.stream()
			.map(this::toDto)
			.toList();
	}

	@Override
	public NotificationTemplateDTO get(Long id) {
		NotificationTemplate template = templateMapper.selectById(id);
		if (template == null)
			throw new BusinessException(404, "通知模板不存在");
		return toDto(template);
	}

	@Override
	public NotificationTemplateDTO getInternally(Long id) {
		return get(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public NotificationTemplateDTO save(NotificationTemplateDTO dto) {
		normalizeAndValidate(dto);
		NotificationTemplate template = new NotificationTemplate();
		BeanUtils.copyProperties(dto, template);
		try {
			templateMapper.insert(template);
		}
		catch (DuplicateKeyException e) {
			throw new BusinessException(409, "通知模板编码已存在");
		}
		replaceRecipients(template.getId(), dto.getRecipients());
		return toDto(template);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public NotificationTemplateDTO update(Long id, NotificationTemplateDTO dto) {
		if (templateMapper.selectById(id) == null)
			throw new BusinessException(404, "通知模板不存在");
		normalizeAndValidate(dto);
		NotificationTemplate template = new NotificationTemplate();
		BeanUtils.copyProperties(dto, template);
		template.setId(id);
		try {
			templateMapper.updateById(template);
		}
		catch (DuplicateKeyException e) {
			throw new BusinessException(409, "通知模板编码已存在");
		}
		replaceRecipients(id, dto.getRecipients());
		return toDto(templateMapper.selectById(id));
	}

	@Override
	public void updateStatus(Long id, Status status) {
		if (templateMapper.update(null,
				new LambdaUpdateWrapper<NotificationTemplate>().eq(NotificationTemplate::getId, id)
					.set(NotificationTemplate::getStatus, status)) == 0) {
			throw new BusinessException(404, "通知模板不存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		recipientMapper.delete(new LambdaQueryWrapper<NotificationTemplateRecipient>()
			.eq(NotificationTemplateRecipient::getTemplateId, id));
		if (templateMapper.deleteById(id) == 0)
			throw new BusinessException(404, "通知模板不存在");
	}

	private void replaceRecipients(Long templateId, List<NotificationTemplateRecipientDTO> values) {
		recipientMapper.delete(new LambdaQueryWrapper<NotificationTemplateRecipient>()
			.eq(NotificationTemplateRecipient::getTemplateId, templateId));
		if (values == null)
			return;
		for (NotificationTemplateRecipientDTO dto : values) {
			if (dto == null || dto.getSelectorType() == null || dto.getSelectorValue() == null
					|| dto.getSelectorValue().isBlank())
				continue;
			NotificationTemplateRecipient row = new NotificationTemplateRecipient();
			row.setTemplateId(templateId);
			row.setSelectorType(dto.getSelectorType());
			row.setSelectorValue(dto.getSelectorValue().trim());
			row.setIncludeChildren(dto.isIncludeChildren());
			recipientMapper.insert(row);
		}
	}

	private void normalizeAndValidate(NotificationTemplateDTO dto) {
		if (dto.getStatus() == null) {
			dto.setStatus(Status.ENABLE);
		}
		if (dto.getNotificationType() == null) {
			dto.setNotificationType(NotificationType.SYSTEM);
		}
		boolean hasPresetRecipient = dto.getRecipients() != null && dto.getRecipients()
			.stream()
			.anyMatch(value -> value != null && value.getSelectorType() != null && value.getSelectorValue() != null
					&& !value.getSelectorValue().isBlank());
		if (dto.getStatus() == Status.ENABLE && !hasPresetRecipient) {
			throw new BusinessException(422, "启用的通知模板必须配置至少一个收件人");
		}
	}

	private NotificationTemplateDTO toDto(NotificationTemplate template) {
		NotificationTemplateDTO dto = new NotificationTemplateDTO();
		BeanUtils.copyProperties(template, dto);
		dto.setRecipients(recipientMapper
			.selectList(new LambdaQueryWrapper<NotificationTemplateRecipient>()
				.eq(NotificationTemplateRecipient::getTemplateId, template.getId())
				.orderByAsc(NotificationTemplateRecipient::getId))
			.stream()
			.map(row -> {
				NotificationTemplateRecipientDTO value = new NotificationTemplateRecipientDTO();
				value.setSelectorType(row.getSelectorType());
				value.setSelectorValue(row.getSelectorValue());
				value.setIncludeChildren(Boolean.TRUE.equals(row.getIncludeChildren()));
				return value;
			})
			.toList());
		return dto;
	}

}
