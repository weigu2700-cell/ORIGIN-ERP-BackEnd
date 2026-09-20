package org.smart.erp.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Web 接口使用 Jackson 3；保持雪花 ID 与日期时间的既有 JSON 格式。 */
@Configuration
public class ApiJacksonConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer snowflakeIdJsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("snowflake-id-as-string");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // 保持升级前接口的空格日期格式，同时接受浏览器常见的 ISO 日期格式。
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            module.addDeserializer(LocalDateTime.class, new ValueDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
                    String value = parser.getValueAsString();
                    if (value == null || value.isBlank()) return null;
                    value = value.trim();
                    for (DateTimeFormatter formatter : new DateTimeFormatter[] {
                            DATE_TIME_FORMATTER, DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    }) {
                        try {
                            return LocalDateTime.parse(value, formatter);
                        } catch (DateTimeParseException ignored) {
                            // 尝试下一种格式。
                        }
                    }
                    throw context.weirdStringException(value, LocalDateTime.class, "无法解析日期时间");
                }
            });
            builder.addModule(module);
        };
    }
}
