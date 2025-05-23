package com.slg.module.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//本地时间工具 5s 误差
public class SystemTimeCache {
    private static volatile long currentTimeMillis;
    static {
        currentTimeMillis = System.currentTimeMillis();
        // 启动定时任务，每10毫秒更新一次缓存时间
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() ->
                        currentTimeMillis = System.currentTimeMillis(),
                0, 3000, TimeUnit.MILLISECONDS
        );
    }

    // 直接返回缓存时间，无方法调用开销
    public static long currentTimeMillis() {
        return currentTimeMillis;
    }
}
