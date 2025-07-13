package com.slg.module.rpc.interMsg;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 高性能多维度 QPS 监控器
 * 支持按不同维度（如 serverId）统计 QPS
 */
public class QPSMonitor {
    // 主监控器名称
//    private final String name;

    // 全局总 QPS 计数器
    private final LongAdder totalCounter = new LongAdder();

    // 按 serverId 分类的计数器 (使用 ConcurrentHashMap 存储每个 serverId 的计数器)
    private final ConcurrentHashMap<Integer, LongAdder> serverCounters = new ConcurrentHashMap<>();

    // 当前各 serverId 的 QPS (使用 ConcurrentHashMap 存储每个 serverId 的 QPS)
    private final ConcurrentHashMap<Integer, AtomicLong> serverQPS = new ConcurrentHashMap<>();

    // 最后更新时间
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());

    // 是否已启动
    private boolean started = false;

    // 单例实例
//    private static final ConcurrentHashMap<String, QPSMonitor> instances = new ConcurrentHashMap<>();

//    /**
//     * 获取命名实例
//     */
//    public static QPSMonitor getInstance(String name) {
//        return instances.computeIfAbsent(name, QPSMonitor::new);
//    }

    // 静态内部类持有单例
    private static class Holder {
        static final QPSMonitor INSTANCE = new QPSMonitor();
    }

    // 全局访问点
    public static QPSMonitor getInstance() {
        return QPSMonitor.Holder.INSTANCE;
    }

    /**
     * 私有构造函数
     */
//    private QPSMonitor(String name) {
//        this.name = name;
//    }
    private QPSMonitor() {
    }


    /**
     * 启动监控器
     *
     * @param eventLoop 在指定的 EventLoop 上调度统计任务
     */
    public synchronized void start(io.netty.util.concurrent.EventExecutor eventLoop) {
        if (!started) {
            // 启动定期统计任务
            eventLoop.scheduleAtFixedRate(this::updateAllQPS, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
            started = true;
        }
    }

    /**
     * 记录一次请求，带 serverId 维度
     */
    public void record(int serverId) {
        // 增加总计数
        totalCounter.increment();

        // 增加对应 serverId 的计数 (使用 computeIfAbsent 确保线程安全)
        serverCounters.computeIfAbsent(serverId, k -> new LongAdder()).increment();
    }

    /**
     * 更新所有维度的 QPS 统计
     */
    private void updateAllQPS() {
        long now = System.currentTimeMillis();
        long lastTime = lastUpdateTime.getAndSet(now);
        long duration = now - lastTime;

        // 更新全局 QPS
        long totalCount = totalCounter.sumThenReset();
        long globalQPS = duration > 0 ? (totalCount * 1000) / duration : 0;

        // 更新每个 serverId 的 QPS
        for (Map.Entry<Integer, LongAdder> entry : serverCounters.entrySet()) {
            int serverId = entry.getKey();
            LongAdder counter = entry.getValue();
            long count = counter.sumThenReset();
            // 使用 computeIfAbsent 确保 serverQPS 中存在该 serverId 的计数器
            serverQPS.computeIfAbsent(serverId, k -> new AtomicLong(0))
                    .set(duration > 0 ? (count * 1000) / duration : 0);
        }

        // 可添加日志输出或发布到监控系统
        System.out.printf("全局QPS: %d%n", globalQPS);
        for (Map.Entry<Integer, AtomicLong> entry : serverQPS.entrySet()) {
            System.out.printf("ServerID=%d 的QPS: %d%n", entry.getKey(), entry.getValue().get());
        }

        //根据qps移除连接
//        if (qps < 10000) {//减少连接
//            removeOneChanel(serverId);
//        }
    }

    /**
     * 获取全局 QPS 值
     */
    public long getGlobalQPS() {
        // 从 serverQPS 中获取，确保数据是更新过的
        return serverQPS.computeIfAbsent(-1, k -> new AtomicLong(0)).get();
    }

    /**
     * 获取指定 serverId 的 QPS 值
     */
    public long getQPS(int serverId) {
        return serverQPS.getOrDefault(serverId, new AtomicLong(0)).get();
    }

    /**
     * 获取所有 serverId 的 QPS 统计
     */
    public Map<Integer, Long> getAllServerQPS() {
        Map<Integer, Long> result = new HashMap<>();
        for (Map.Entry<Integer, AtomicLong> entry : serverQPS.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

//    /**
//     * 获取监控器名称
//     */
//    public String getName() {
//        return name;
//    }
}