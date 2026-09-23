package org.smart.erp.ai.rag;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "rag_document")
public class RagDocument {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String docName;

    private String fileType;

    private Integer chunkCount;

    /** ACTIVE / DELETED */
    private String status;

    private String uploadedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
