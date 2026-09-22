package org.smart.erp.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private  Retrieve retrieve = new Retrieve();

    @Data
    public static class Retrieve{

        private int topK = 4;
        private double minScore = 0.5;
    }
}
