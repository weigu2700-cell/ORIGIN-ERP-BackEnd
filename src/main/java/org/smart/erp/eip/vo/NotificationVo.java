package org.smart.erp.eip.vo;

import lombok.Data;
import org.smart.erp.eip.enums.NotificationType;

import java.time.LocalDateTime;

/** 收件箱 REST/WS 视图；属性名保持原客户端 JSON 合同。 */
@Data
public class NotificationVo {

	private Long id;

	private Long userId;

	private NotificationType type;

	private String title;

	private String content;

	private Long businessId;

	private String businessNo;

	private String businessType;

	private Boolean isRead;

	private LocalDateTime readTime;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

}