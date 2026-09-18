package org.smart.erp.eip.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.enums.RecipientMergeMode;

import java.util.LinkedHashMap;
import java.util.Map;

/** 系统通知发布参数对象（基于模板或显式内容）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Jacksonized
public class SystemNotificationPublishDTO extends AbstractNotificationPublishDTO {

	private Long templateId;

	private String templateCode;

	private RecipientMergeMode mergeMode;

	@Builder.Default
	private Map<String, String> variables = new LinkedHashMap<>();

	private String businessType;

	private Long businessId;

	private String businessNo;

	@AssertTrue(message = "templateId、templateCode或title/content至少提供一组")
	public boolean hasSource() {
		String title = getTitle();
		String content = getContent();
		return templateId != null || (templateCode != null && !templateCode.isBlank())
				|| (title != null && !title.isBlank() && content != null);
	}

}
