package org.smart.erp.eip.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.smart.erp.eip.enums.NotificationType;

import java.time.LocalDateTime;

/** 个人收件箱记录；字段与既有 sys_notification JSON 合同保持一致。 */
@Data
@TableName("sys_notification")
public class Notification {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @JsonIgnore
    private Long publishId;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private Long businessId;
    private String businessNo;
    private String businessType;
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
