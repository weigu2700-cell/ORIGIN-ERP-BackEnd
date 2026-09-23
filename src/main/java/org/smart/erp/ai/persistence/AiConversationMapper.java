package org.smart.erp.ai.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.ai.persistence.AiConversation;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
