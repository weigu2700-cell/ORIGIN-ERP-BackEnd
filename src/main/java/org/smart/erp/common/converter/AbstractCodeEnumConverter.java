package org.smart.erp.common.converter;

import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 公共基类：将查询参数中的字符串（数字码或枚举名）转换为带数字 code 的枚举。
 *
 * <p>所有业务状态码枚举（如 ProductionOrderStatus、PurchaseOrderStatus 等）都只需要在
 * 各自模块的 converter 包下写一个继承本类的空壳类，并传入枚举类型即可，避免每个枚举
 * 重复编写相同的 convert 逻辑。</p>
 *
 * <p>转换规则：优先按数字 code 匹配（通过反射读取枚举的 getCode()），命中则返回；
 * 否则按枚举名（忽略大小写）匹配；都失败返回 null。</p>
 */
public abstract class AbstractCodeEnumConverter<T extends Enum<T>> implements Converter<String, T> {

    private final Class<T> enumType;
    private final Map<Integer, T> codeToEnum = new HashMap<>();

    protected AbstractCodeEnumConverter(Class<T> enumType) {
        this.enumType = enumType;
        Method getCodeMethod = null;
        try {
            getCodeMethod = enumType.getMethod("getCode");
        } catch (NoSuchMethodException ignored) {
            // 该枚举没有 code 字段，仅靠枚举名匹配
        }
        if (getCodeMethod != null) {
            for (T constant : enumType.getEnumConstants()) {
                try {
                    Object codeValue = getCodeMethod.invoke(constant);
                    if (codeValue instanceof Number) {
                        codeToEnum.put(((Number) codeValue).intValue(), constant);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // 跳过无法读取的常量
                }
            }
        }
    }

    @Override
    public T convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        try {
            int code = Integer.parseInt(trimmed);
            T matched = codeToEnum.get(code);
            if (matched != null) {
                return matched;
            }
        } catch (NumberFormatException ignored) {
            // 非数字，继续按名称匹配
        }
        try {
            return Enum.valueOf(enumType, trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
