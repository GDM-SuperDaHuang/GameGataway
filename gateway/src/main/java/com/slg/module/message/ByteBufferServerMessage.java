package com.slg.module.message;

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
    //    private ByteBuffer body;
    private byte[] body;

    public ByteBufferServerMessage() {
    }

    public ByteBufferServerMessage(long userId, int cid, int errorCode, int protocolId, byte[] body) {
        this.userId = userId;
        this.cid = cid;
        this.errorCode = errorCode;
        this.protocolId = protocolId;
        this.body = body;
    }

    public ByteBufferServerMessage(long userId, int cid, int errorCode, int protocolId, byte zip, byte encrypted, byte[] body) {
        this.userId = userId;
        this.cid = cid;
        this.errorCode = errorCode;
        this.protocolId = protocolId;
        this.zip = zip;
        this.encrypted = encrypted;
        this.body = body;
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

    public byte[] getBody() {
        return body;
    }
}
