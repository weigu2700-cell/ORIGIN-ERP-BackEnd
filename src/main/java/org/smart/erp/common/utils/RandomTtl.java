package org.smart.erp.common.utils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成带随机抖动的 TTL，避免大量缓存同一时刻集中过期引发"缓存雪崩"。
 */
public final class RandomTtl {

    private RandomTtl() {
    }

    /**
     * 生成 [minMinutes, maxMinutes] 闭区间内的随机分钟级 TTL。
     */
    public static Duration ofMinutes(long minMinutes, long maxMinutes) {
        if (minMinutes > maxMinutes) {
            throw new IllegalArgumentException("minMinutes 必须 <= maxMinutes");
        }
        long minutes = ThreadLocalRandom.current().nextLong(minMinutes, maxMinutes + 1);
        return Duration.ofMinutes(minutes);
    }
}
