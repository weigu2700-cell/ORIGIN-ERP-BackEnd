package org.smart.erp.common.utils;

/**
 * 雪花算法 ID 生成器（Twitter Snowflake）。
 * <p>
 * 结构（64位）：符号位(1) + 时间戳(41) + 机器id(10) + 序列号(12)。
 * 提供 {@link #nextId()} 返回完整 long，和 {@link #nextIdSuffix(int)} 截取尾部若干位用于业务编码。
 */
public final class SnowflakeIdGenerator {

    /** 起始时间戳（2024-01-01），可根据项目实际调整 */
    private static final long EPOCH = 1_704_067_200_000L;

    /** 机器 id 占 10 位（5 位 datacenter + 5 位 worker），最大 1023 */
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        // 单机默认机器/数据中心 id，集群部署时可从配置注入
        this(1L, 1L);
    }

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出范围 0~" + MAX_WORKER_ID);
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 超出范围 0~" + MAX_DATACENTER_ID);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成 id");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成完整雪花 id 并截取尾部 length 位（不足补前导 0），用于拼业务编码后缀。
     */
    public String nextIdSuffix(int length) {
        String full = String.valueOf(nextId());
        if (full.length() <= length) {
            return String.format("%0" + length + "d", Long.parseLong(full));
        }
        return full.substring(full.length() - length);
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
