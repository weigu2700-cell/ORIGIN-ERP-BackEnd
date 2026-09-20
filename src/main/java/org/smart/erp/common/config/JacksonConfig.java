package org.smart.erp.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 全局配置。
 *
 * <p>这里配置 Jackson 2 的 {@code ObjectMapper}，供 Redis 等仍使用 Jackson 2 的组件使用。
 * Spring MVC 在 Spring Boot 4 中使用 Jackson 3，其 ID 与日期时间配置见 {@link ApiJacksonConfig}。</p>
 *
 * <p>将 Long / long 类型序列化为字符串，避免雪花 ID（超过 JS 安全整数范围 2^53）传给前端时精度丢失，
 * 导致前端回传 id 后后端查不到数据。序列化（后端 -> 前端）统一 LocalDateTime 为 yyyy-MM-dd HH:mm:ss；
 * 反序列化（前端 -> 后端）额外兼容 ISO 格式（yyyy-MM-dd'T'HH:mm:ss）等常见写法。</p>
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 反序列化时依次尝试的常见格式：标准空格格式、ISO 带 T 格式（含/不含毫秒）。 */
    private static final DateTimeFormatter[] DESERIALIZE_FORMATTERS = {
            DATE_TIME_FORMATTER,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /**
     * 显式提供 {@link ObjectMapper} Bean。
     * 复刻 Spring Boot 默认设置，并叠加业务模块（Long -> String、LocalDateTime 格式化）。
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 复刻 Spring Boot 默认 Jackson 行为
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Java 8 时间类型支持
        objectMapper.registerModule(new JavaTimeModule());
        // 业务模块（后注册，覆盖 LocalDateTime 的默认序列化/反序列化）
        objectMapper.registerModule(longToStringModule());
        return objectMapper;
    }

    /**
     * 业务相关的 Jackson 模块：Long -> String、LocalDateTime 格式化与兼容解析。
     * 作为 Bean 保留，便于在 {@link #objectMapper()} 中复用，也可被 Jackson 自动配置加载。
     */
    @Bean
    public SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getValueAsString();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                String value = text.trim();
                for (DateTimeFormatter formatter : DESERIALIZE_FORMATTERS) {
                    try {
                        return LocalDateTime.parse(value, formatter);
                    } catch (DateTimeParseException ignored) {
                    }
                }
                throw new IOException("无法解析日期时间: " + value);
            }
        });
        return module;
    }
}
