package org.smart.erp.ai.dto.toolResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Stable, strongly typed page shape returned by AI query tools.
 */
public record PageToolResult<T>(
        List<T> records,
        long total,
        long pageNum,
        long pageSize
) {

    public static <T> PageToolResult<T> from(Page<T> page) {
        return new PageToolResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }
}
