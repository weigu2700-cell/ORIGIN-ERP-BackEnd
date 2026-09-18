package org.smart.erp.eip.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_publish")
public class NotificationPublish {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** EIP 业务发布幂等键。 */
    private String requestId;
    private NotificationSourceType sourceType;
    private NotificationType type;

    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Integer recipientCount;
    @com.baomidou.mybatisplus.annotation.TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @com.baomidou.mybatisplus.annotation.TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
