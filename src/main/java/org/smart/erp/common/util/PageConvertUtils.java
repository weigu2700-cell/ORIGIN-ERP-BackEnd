package org.smart.erp.common.util;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页对象转换工具，消除各 Service 中 "entity 分页 -> VO 分页" 的重复代码。
 */
public final class PageConvertUtils {

    private PageConvertUtils() {
    }

    /**
     * 直接将 entity 字段拷贝到目标 VO（要求 entity 与 vo 同名字段）。
     *
     * @param page    原 entity 分页
     * @param voClass VO 类型
     * @param <E>     entity 类型
     * @param <V>     vo 类型
     * @return 转换后的 VO 分页
     */
    public static <E, V> Page<V> convert(Page<E> page, Class<V> voClass) {
        Page<V> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(entity -> {
                    V vo = instantiate(voClass);
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .toList());
        return voPage;
    }

    /**
     * 使用自定义转换器逐个转换记录。
     *
     * @param page   原 entity 分页
     * @param mapper 单条 entity -> vo 的转换函数
     * @param <E>    entity 类型
     * @param <V>    vo 类型
     * @return 转换后的 VO 分页
     */
    public static <E, V> Page<V> convert(Page<E> page, Function<E, V> mapper) {
        Page<V> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(mapper).toList());
        return voPage;
    }

    private static <V> V instantiate(Class<V> voClass) {
        try {
            return voClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("VO 类型必须有无参构造器: " + voClass.getName(), e);
        }
    }
}
