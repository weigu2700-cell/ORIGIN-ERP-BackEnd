package org.smart.erp.ai.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.ai.persistence.AiMessage;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
