package com.slg.module.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端与服务器之间协议
 */
public class ByteBufferMessage {
//    // 统计命中率
//    private static final AtomicLong HIT_COUNT = new AtomicLong(0);
//    private static final AtomicLong MISS_COUNT = new AtomicLong(0);
//
//    private static void testCount(ByteBufferMessage msg) {
//        if (msg.protocolId == 0) {
//            MISS_COUNT.incrementAndGet();
//        } else {
//            HIT_COUNT.incrementAndGet();
//        }
//    }
//
//    // 获取命中率
//    public static double getHitRate() {
//        long hit = HIT_COUNT.get();
//        long miss = MISS_COUNT.get();
//        long total = hit + miss;
//        return total == 0 ? 0.0 : (double) hit / total;
//    }
//
//    // 打印统计信息
//    public static void printStats() {
//        System.out.printf(
//                "ByteBufferMessage Pool Stats: Hits=%d, Misses=%d, Hit Rate=%.2f%%\n",
//                HIT_COUNT.get(),
//                MISS_COUNT.get(),
//                getHitRate() * 100
//        );
//    }


    private int cid;//顺序号
    private int errorCode;//错误码
    private int protocolId;//协议id
    private byte zip;
    private byte encrypted;
    private short length;//长度
    //private ByteBuffer body;//消息体
    //private byte[] body;//消息体
    private ByteBuf body; // 改用 ByteBuf 避免拷贝

    private static final Recycler<ByteBufferMessage> RECYCLER = new Recycler<ByteBufferMessage>() {
        @Override
        protected ByteBufferMessage newObject(Handle<ByteBufferMessage> handle) {
            return new ByteBufferMessage(handle);
        }
    };

    private final Recycler.Handle<ByteBufferMessage> handle; // final字段

    // 必须的私有构造器
    private ByteBufferMessage(Recycler.Handle<ByteBufferMessage> handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    // 从对象池获取实例（传入 ByteBuf 直接引用）
    public static ByteBufferMessage newInstance(int cid, int errorCode, int protocolId, byte zip, byte encrypted, short length, ByteBuf body) {
        ByteBufferMessage msg = RECYCLER.get();
        //测试命中
//        testCount(msg);

        msg.cid = cid;
        msg.errorCode = errorCode;
        msg.protocolId = protocolId;
        msg.zip = zip;
        msg.encrypted = encrypted;
        msg.length = length;
        msg.body = body; // 直接引用，不调用 retain()
//        msg.body = body.touch();  // 记录泄漏检测信息
        return msg;
    }

    // 回收对象
    public void recycle() {
        cid = 0;
        errorCode = 0;
        protocolId = 0;
        if (body != null) {
            body.release();
            body = null;
        }
        handle.recycle(this);
    }

    public short getLength() {
        return length;
    }

    public int getCid() {
        return cid;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getProtocolId() {
        return protocolId;
    }

    public ByteBuf getBody() {
        return body;
    }

    public byte getZip() {
        return zip;
    }

    public byte getEncrypted() {
        return encrypted;
    }
}
