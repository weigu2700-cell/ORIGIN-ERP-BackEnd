package org.smart.erp.im;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class ImJson {

    // 独立、无多态类型的协议编解码器，不改变旧 ERP 接口的兼容规则。
    private final ObjectMapper mapper = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .withCoercionConfig(com.fasterxml.jackson.databind.type.LogicalType.Textual, config -> {
            config.setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,
                    com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
            config.setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,
                    com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
            config.setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,
                    com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
        })
        .build();

    public <T> T read(String value, Class<T> type) {
        try {
            if (type == ImModels.SendMessage.class) {
                var node = mapper.readTree(value);
                var names = new java.util.HashSet<String>();
                node.fieldNames().forEachRemaining(names::add);
                var expected = "TEXT".equals(node.path("type").asText())
                        ? java.util.Set.of("clientMessageId", "type", "text")
                        : java.util.Set.of("clientMessageId", "type", "fileId");
                if (!names.equals(expected))
                    throw new IllegalArgumentException();
            }
            return mapper.readValue(value, type);
        }
        catch (Exception e) {
            throw ImException.invalid("JSON字段、类型或格式不正确");
        }
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new IllegalStateException("IM协议编码失败", e);
        }
    }

    public Object object(String value) {
        return read(value, Object.class);
    }

    public String hash(Object value) {
        return sha256(write(value));
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
