package org.smart.erp.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.ai.rag.entity.RagChunk;

@Mapper
public interface RagChunkMapper extends BaseMapper<RagChunk> {
}
