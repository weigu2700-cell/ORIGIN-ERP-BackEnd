package org.smart.erp.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 全局配置：
 * 将 Long / long 类型序列化为字符串，避免雪花 ID（超过 JS 安全整数范围 2^53）
 * 传给前端时发生精度丢失，导致前端回传 id 后后端查不到数据。
 * 序列化（后端 -> 前端）统一 LocalDateTime 为 yyyy-MM-dd HH:mm:ss；
 * 反序列化（前端 -> 后端）额外兼容 ISO 格式（yyyy-MM-dd'T'HH:mm:ss）等常见写法，
 * 避免前端按不同格式传参时反序列化失败。
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

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
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
        };
    }
}
