package com.slg.module.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestMsg {
    // 存储不同 protocolId 对应的统计器实例
    private static final ConcurrentMap<Integer, TestMsg> INSTANCES = new ConcurrentHashMap<>();

    // 私有构造方法，禁止外部直接创建
    private TestMsg() {}

    // 获取指定 protocolId 的统计器实例（线程安全）
    public static TestMsg getInstance(int protocolId) {
        return INSTANCES.computeIfAbsent(protocolId, id -> new TestMsg());
    }

    // 成员变量：每个实例独立统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    // 记录命中/未命中
    public void testCount(ByteBufferMessage msg) {
        if (msg.getProtocolId() == 0) {
            missCount.incrementAndGet();
        } else {
            hitCount.incrementAndGet();
        }
    }

    // 获取当前实例的命中率
    public double getHitRate() {
        long hit = hitCount.get();
        long miss = missCount.get();
        long total = hit + miss;
        return total == 0 ? 0.0 : (double) hit / total;
    }

    // 打印当前实例的统计信息
    public void printStats() {
        System.out.printf(
                "Protocol Stats [ID=%d]: Hits=%d, Misses=%d, Hit Rate=%.2f%%\n",
                getProtocolId(),  // 需要额外方法获取当前 protocolId
                hitCount.get(),
                missCount.get(),
                getHitRate() * 100
        );
    }

    // 获取当前实例对应的 protocolId（需反向查找）
    public int getProtocolId() {
        return INSTANCES.entrySet().stream()
                .filter(entry -> entry.getValue() == this)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);  // 未找到时返回-1
    }

    // 打印所有实例的统计信息
    public static void printAllStats() {
        INSTANCES.forEach((id, instance) -> instance.printStats());
    }
}
