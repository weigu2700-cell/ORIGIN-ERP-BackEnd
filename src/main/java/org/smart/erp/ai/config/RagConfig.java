package org.smart.erp.ai.config;

import org.smart.erp.ai.mapper.RagChunkMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

}
