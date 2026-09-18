package org.smart.erp.eip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.dto.NotificationTemplateDTO;
import org.smart.erp.eip.dto.NotificationTemplateRecipientDTO;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.smart.erp.eip.entity.NotificationTemplate;
import org.smart.erp.eip.enums.RecipientMergeMode;
import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.dto.NotificationBusinessRefDTO;
import org.smart.erp.eip.mapper.NotificationTemplateMapper;
import org.smart.erp.eip.service.NotificationPublisher;
import org.smart.erp.eip.service.NotificationRecipientResolver;
import org.smart.erp.eip.service.NotificationTemplateService;
import org.smart.erp.eip.service.SystemNotificationService;
import org.smart.erp.system.Enum.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SystemNotificationServiceImpl implements SystemNotificationService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Za-z0-9_]+)}}");
    private final NotificationTemplateMapper templateMapper;
    private final NotificationTemplateService templateService;
    private final NotificationRecipientResolver resolver;
    private final NotificationPublisher publisher;

    public SystemNotificationServiceImpl(NotificationTemplateMapper templateMapper,
                                         NotificationTemplateService templateService,
                                         NotificationRecipientResolver resolver,
                                         NotificationPublisher publisher) {
        this.templateMapper = templateMapper;
        this.templateService = templateService;
        this.resolver = resolver;
        this.publisher = publisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publish(SystemNotificationPublishDTO dto) {
        NotificationTemplate template = findTemplate(dto);
        NotificationTemplateDTO templateDto = template == null ? null : templateService.get(template.getId());
        String title = render(dto.getTitle() != null ? dto.getTitle() : required(templateDto, true), dto.getVariables());
        String content = render(dto.getContent() != null ? dto.getContent() : required(templateDto, false), dto.getVariables());
        RecipientSelectorDTO preset = fromTemplate(templateDto);
        RecipientSelectorDTO explicit = dto.getRecipients() == null ? new RecipientSelectorDTO() : dto.getRecipients();
        RecipientMergeMode mergeMode = dto.getMergeMode() != null
                ? dto.getMergeMode()
                : template == null ? RecipientMergeMode.OVERRIDE : RecipientMergeMode.PRESET_ONLY;
        Set<Long> recipients = resolveRecipients(mergeMode, preset, explicit);
        if (recipients.isEmpty()) throw new BusinessException(422, "通知收件人不能为空或均未启用");

        String requestId = dto.getRequestId() == null || dto.getRequestId().isBlank()
                ? UUID.randomUUID().toString() : dto.getRequestId().trim();
        NotificationPublishDTO event = NotificationPublishDTO.builder()
                .requestId(requestId)
                .sourceType(NotificationSourceType.SYSTEM)
                .type(dto.getType() != null ? dto.getType()
                        : template == null ? org.smart.erp.eip.enums.NotificationType.SYSTEM : template.getNotificationType())
                .title(title)
                .content(content)
                .business(NotificationBusinessRefDTO.builder()
                        .businessType(dto.getBusinessType())
                        .businessId(dto.getBusinessId())
                        .businessNo(dto.getBusinessNo())
                        .build())
                .build();
        RecipientSelectorDTO selectedRecipients = new RecipientSelectorDTO();
        selectedRecipients.setUserIds(recipients);
        event.setRecipients(selectedRecipients);
        publisher.publish(event);
        return requestId;
    }

    private NotificationTemplate findTemplate(SystemNotificationPublishDTO dto) {
        if (dto.getTemplateId() != null) return enabledOrFail(templateMapper.selectById(dto.getTemplateId()));
        if (dto.getTemplateCode() != null && !dto.getTemplateCode().isBlank()) {
            return enabledOrFail(templateMapper.selectOne(new LambdaQueryWrapper<NotificationTemplate>()
                    .eq(NotificationTemplate::getCode, dto.getTemplateCode().trim())));
        }
        return null;
    }

    private NotificationTemplate enabledOrFail(NotificationTemplate template) {
        if (template == null) throw new BusinessException(404, "通知模板不存在");
        if (template.getStatus() != Status.ENABLE) throw new BusinessException(409, "通知模板已停用");
        return template;
    }

    private String required(NotificationTemplateDTO template, boolean title) {
        if (template == null) throw new BusinessException(422, "未提供通知模板或标题/正文");
        return title ? template.getTitleTemplate() : template.getContentTemplate();
    }

    private Set<Long> resolveRecipients(RecipientMergeMode mode, RecipientSelectorDTO preset,
                                        RecipientSelectorDTO explicit) {
        Set<Long> result = new LinkedHashSet<>();
        if (mode == RecipientMergeMode.PRESET_ONLY || mode == RecipientMergeMode.MERGE) result.addAll(resolver.resolve(preset));
        if (mode == RecipientMergeMode.OVERRIDE || mode == RecipientMergeMode.MERGE) result.addAll(resolver.resolve(explicit));
        return result;
    }

    private RecipientSelectorDTO fromTemplate(NotificationTemplateDTO template) {
        RecipientSelectorDTO result = new RecipientSelectorDTO();
        if (template == null || template.getRecipients() == null) return result;
        for (NotificationTemplateRecipientDTO row : template.getRecipients()) {
            if (row == null || row.getSelectorType() == null) continue;
            String[] values = row.getSelectorValue() == null ? new String[0] : row.getSelectorValue().split(",");
            switch (row.getSelectorType()) {
                case USER -> addLongs(values, result.getUserIds());
                case ROLE -> addStrings(values, result.getRoleCodes());
                case PERMISSION -> addStrings(values, result.getPermissionCodes());
                case DEPARTMENT -> addLongs(values, result.getDepartmentIds());
                case ALL_ACTIVE_USERS -> result.setAllActiveUsers(true);
                case ADMINISTRATORS -> result.setIncludeAdministrators(true);
            }
        }
        return result;
    }

    private void addLongs(String[] values, Set<Long> target) {
        for (String value : values) {
            try { target.add(Long.valueOf(value.trim())); }
            catch (NumberFormatException e) { throw new BusinessException(422, "收件人选择器ID格式错误"); }
        }
    }

    private void addStrings(String[] values, Set<String> target) {
        for (String value : values) if (!value.isBlank()) target.add(value.trim());
    }

    static String render(String source, Map<String, String> variables) {
        if (source == null) throw new BusinessException(422, "通知标题或正文不能为空");
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (variables == null || !variables.containsKey(name) || variables.get(name) == null) {
                throw new BusinessException(422, "通知模板变量缺失: " + name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(variables.get(name)));
        }
        matcher.appendTail(result);
        if (result.indexOf("{{") >= 0 || result.indexOf("}}") >= 0) {
            throw new BusinessException(422, "仅支持 {{name}} 格式的模板变量");
        }
        return result.toString();
    }
}
