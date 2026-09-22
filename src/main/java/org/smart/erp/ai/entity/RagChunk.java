package org.smart.erp.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "rag_chunk")
public class RagChunk {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private Integer chunkSeq;

    private String content;

    private String metadataJson;

    /** 向量本体，JSON float 数组字符串 */
    private String embedding;

    private Integer embeddingDim;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
