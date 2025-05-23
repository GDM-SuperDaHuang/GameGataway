package com.slg.module.message;

import com.google.protobuf.GeneratedMessage;
import io.netty.util.Recycler;

import java.util.Objects;

public class MsgResponse {
    private int errorCode;
    private GeneratedMessage.Builder<?> body;

    private static final Recycler<MsgResponse> RECYCLER = new Recycler<MsgResponse>() {
        @Override
        protected MsgResponse newObject(Handle<MsgResponse> handle) {
            return new MsgResponse(handle);
        }
    };

    private final Recycler.Handle<MsgResponse> handle; // final字段

    // 必须的私有构造器
    private MsgResponse(Recycler.Handle<MsgResponse> handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    // 从对象池获取实例（传入 ByteBuf 直接引用）
    public static MsgResponse newInstance(int errorCode, GeneratedMessage.Builder<?> body) {
        MsgResponse msg = RECYCLER.get();
        msg.errorCode = errorCode;
        msg.body = body;
        return msg;
    }

    // 回收对象
    public void recycle() {
        errorCode = 0;
        body = null;
        handle.recycle(this);
    }


    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public GeneratedMessage.Builder<?> getBody() {
        return body;
    }

    public void setBody(GeneratedMessage.Builder<?> body) {
        this.body = body;
    }
}
