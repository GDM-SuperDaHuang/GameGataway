package com.slg.module.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;

import java.util.Objects;

/**
 * 服务器内部协议
 */
public class ByteBufferServerMessage {
    private long userId;//用户id
    private int cid;//顺序号
    private int errorCode;//错误码
    private int protocolId;
    private byte zip;//压缩
    private byte encrypted;//加密
    private short length;//长度
    private ByteBuf body; // 改用 ByteBuf 避免拷贝
    private static final Recycler<ByteBufferServerMessage> RECYCLER = new Recycler<ByteBufferServerMessage>() {
        @Override
        protected ByteBufferServerMessage newObject(Handle<ByteBufferServerMessage> handle) {
            return new ByteBufferServerMessage(handle);
        }
    };

    private final Recycler.Handle<ByteBufferServerMessage> handle; // final字段

    // 必须的私有构造器
    private ByteBufferServerMessage(Recycler.Handle<ByteBufferServerMessage> handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    // 从对象池获取实例（传入 ByteBuf 直接引用）
    public static ByteBufferServerMessage newInstance(long userId, int cid, int errorCode, int protocolId, byte zip, byte encrypted, short length, ByteBuf body) {
        ByteBufferServerMessage msg = RECYCLER.get();
        msg.userId = userId;
        msg.cid = cid;
        msg.errorCode = errorCode;
        msg.protocolId = protocolId;
        msg.zip = zip;
        msg.encrypted = encrypted;
        msg.length = length;
        msg.body = body; // 增加引用计数
//        msg.body = body.retain(); // 增加引用计数
        return msg;
    }

//    // 解析 Protobuf（零拷贝）
//    public <T extends com.google.protobuf.Message> T parseBody(com.google.protobuf.Parser<T> parser)
//            throws InvalidProtocolBufferException {
//        return parser.parseFrom(body.nioBuffer());
//    }

    // 回收对象
    public void recycle() {
        userId = 0;
        cid = 0;
        errorCode = 0;
        protocolId = 0;
        zip = 0;
        encrypted = 0;
        length = 0;
        if (body != null) {
            body.release();
            body = null;
        }
        handle.recycle(this);
    }


    public long getUserId() {
        return userId;
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

    public byte getZip() {
        return zip;
    }

    public byte getEncrypted() {
        return encrypted;
    }

    public ByteBuf getBody() {
        return body;
    }

    public short getLength() {
        return length;
    }
}
