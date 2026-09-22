package org.smart.erp.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.ai.rag.entity.RagDocument;

@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {
}
