package org.smart.erp.common.excel;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.read.listener.ReadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Excel 行收集监听器：收敛 FastExcel 逐行回调的样板代码，
 * 读取结束后将全部数据行一次性交给 {@link #consumer} 处理。
 *
 * <p>模板约定：第 1 行为 {@code @ExcelProperty} 表头，数据从第 2 行开始。
 * 因此 {@code rows} 中下标 {@code i} 对应 Excel 第 {@code excelRowNo(i)} 行，
 * 业务方报错定位行号时可直接使用 {@link #excelRowNo(int)}。</p>
 *
 * @param <T> 行数据模型（标注 {@code @ExcelProperty} 的 DTO）
 */
public class ExcelRowCollectListener<T> implements ReadListener<T> {

    /** 模板表头占用的行数 */
    private static final int HEAD_ROW_COUNT = 1;

    private final List<T> rows = new ArrayList<>();
    private final Consumer<List<T>> consumer;

    public ExcelRowCollectListener(Consumer<List<T>> consumer) {
        this.consumer = Objects.requireNonNull(consumer, "consumer must not be null");
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        rows.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        consumer.accept(rows);
    }

    /** 收集列表下标对应的 Excel 实际行号（第 1 行为表头，从第 2 行起为数据） */
    public static int excelRowNo(int index) {
        return HEAD_ROW_COUNT + index + 1;
    }
}
