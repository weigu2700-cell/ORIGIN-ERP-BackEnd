package org.smart.erp.eip.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.system.Enum.Status;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_template")
public class NotificationTemplate {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String titleTemplate;
    private String contentTemplate;
    private NotificationType notificationType;
    private Status status;
    private String remark;
    @com.baomidou.mybatisplus.annotation.TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @com.baomidou.mybatisplus.annotation.TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
