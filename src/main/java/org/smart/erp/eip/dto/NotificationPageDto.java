package org.smart.erp.eip.dto;

import lombok.Data;

/** 收件箱查询参数，字段名称保持原 REST API 兼容。 */
@Data
public class NotificationPageDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Boolean isRead;
}
