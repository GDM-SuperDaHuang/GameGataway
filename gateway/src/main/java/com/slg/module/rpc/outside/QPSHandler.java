package com.slg.module.rpc.outside;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class QPSHandler extends ChannelInboundHandlerAdapter {
    // 静态计数器（所有实例共享）
    private static final AtomicLong requestCount = new AtomicLong(0);
    private static volatile long lastTime = System.currentTimeMillis();
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    // 单例实例
    public static final QPSHandler INSTANCE = new QPSHandler();
    static {
        // 每秒打印 QPS
        scheduler.scheduleAtFixedRate(() -> {
            long current = System.currentTimeMillis();
            long interval = (current - lastTime) / 1000;
            long qps = interval == 0 ? 0 : requestCount.getAndSet(0) / interval;
            lastTime = current;
            if (qps>10000){
                System.out.println("[QPS] " + qps);
            }

        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        requestCount.incrementAndGet();
        ctx.fireChannelRead(msg);
    }
}
