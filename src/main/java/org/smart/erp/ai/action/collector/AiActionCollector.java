package org.smart.erp.ai.action.collector;

import org.smart.erp.ai.action.model.AiActionProposal;
import org.smart.erp.common.exception.BusinessException;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AiActionCollector {

    private final Map<String, AiActionProposal> proposals = new ConcurrentHashMap<>();
    public final static String ACTION_COLLECTOR_KEY = "actionCollector";
    public void add(AiActionProposal aiActionProposal) {
        String key = buildKey(aiActionProposal);
        proposals.putIfAbsent(key, aiActionProposal);
    }

    public List<AiActionProposal> getAll() {
        return List.copyOf(proposals.values());
    }



    private String buildKey(AiActionProposal aiActionProposal) {
        return aiActionProposal.actionType() + ":" + aiActionProposal.bizId();
    }

}
