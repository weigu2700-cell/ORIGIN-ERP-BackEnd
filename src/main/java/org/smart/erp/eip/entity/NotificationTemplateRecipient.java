package org.smart.erp.eip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.smart.erp.eip.enums.RecipientSelectorType;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_template_recipient")
public class NotificationTemplateRecipient {

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	private Long templateId;

	private RecipientSelectorType selectorType;

	private String selectorValue;

	private Boolean includeChildren;

	private LocalDateTime createTime;

}