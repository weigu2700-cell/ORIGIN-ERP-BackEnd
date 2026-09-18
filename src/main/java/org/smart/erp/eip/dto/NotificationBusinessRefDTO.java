package org.smart.erp.eip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 业务通知关联信息，不让业务模块直接依赖收件箱实体。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBusinessRefDTO {

	private String businessType;

	private Long businessId;

	private String businessNo;

	public static NotificationBusinessRefDTO of(String businessType, Long businessId, String businessNo) {
		return NotificationBusinessRefDTO.builder()
			.businessType(businessType)
			.businessId(businessId)
			.businessNo(businessNo)
			.build();
	}

}