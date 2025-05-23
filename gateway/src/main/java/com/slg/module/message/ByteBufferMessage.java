package com.slg.module.message;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;

import java.util.Objects;

/**
 * 客户端与服务器之间协议
 */
public class ByteBufferMessage {
    private int cid;//顺序号
    private int errorCode;//错误码
    private int protocolId;//协议id
    //    private ByteBuffer body;//消息体
//    private byte[] body;//消息体
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
    public static ByteBufferMessage newInstance(int cid, int errorCode, int protocolId, ByteBuf body) {
        ByteBufferMessage msg = RECYCLER.get();
        msg.cid = cid;
        msg.errorCode = errorCode;
        msg.protocolId = protocolId;
        msg.body = body.retain(); // 增加引用计数
        return msg;
    }

    // 解析 Protobuf（零拷贝）
    public <T extends com.google.protobuf.Message> T parseBody(com.google.protobuf.Parser<T> parser)
            throws InvalidProtocolBufferException {
        return parser.parseFrom(body.nioBuffer());
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


//    public ByteBufferMessage() {
//    }

//    public ByteBufferMessage(int cid, int errorCode, int protocolId, byte[] body) {
//        this.cid = cid;
//        this.errorCode = errorCode;
//        this.protocolId = protocolId;
//        this.body = body;
//    }

    public int getCid() {
        return cid;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getProtocolId() {
        return protocolId;
    }

//    public byte[] getBody() {
//        return body;
//    }
    public ByteBuf getBody() {
        return body;
    }
}
