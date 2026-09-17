package org.smart.erp.system.dto;

import lombok.Data;

@Data
public class NotificationPageDto {

    private Integer pageNum;

    private Integer pageSize;

    private Boolean isRead;
}
