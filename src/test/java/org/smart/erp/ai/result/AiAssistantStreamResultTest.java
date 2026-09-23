package org.smart.erp.ai.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiAssistantStreamResultTest {

    @Test
    void keepsTheExistingFrontendSseContract() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                new AiAssistantStreamResult(AiStreamType.CONTENT, "库存充足")));

        assertEquals("CONTENT", json.get("type").asText());
        assertEquals("库存充足", json.get("content").asText());
        assertFalse(json.has("data"));
    }
}
