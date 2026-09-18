package org.smart.erp.eip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.smart.erp.eip.enums.RecipientMergeMode;
import org.smart.erp.eip.enums.NotificationType;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SystemNotificationPublishDTO {
    private String requestId;
    private Long templateId;
    private String templateCode;
    private RecipientMergeMode mergeMode;
    @Valid
    private RecipientSelectorDTO recipients = new RecipientSelectorDTO();
    private Map<String, String> variables = new LinkedHashMap<>();
    private NotificationType type;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String businessNo;

    @AssertTrue(message = "templateId、templateCode或title/content至少提供一组")
    public boolean hasSource() {
        return templateId != null || (templateCode != null && !templateCode.isBlank())
                || (title != null && !title.isBlank() && content != null);
    }
}
