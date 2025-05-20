package com.slg.module.message;

/**
 * 客户端与服务器之间协议
 */
public class ByteBufferMessage {
    private int cid;//顺序号
    private int errorCode;//错误码
    private int protocolId;//协议id
    //    private ByteBuffer body;//消息体
    private byte[] body;//消息体

    public ByteBufferMessage() {
    }

    public ByteBufferMessage(int cid, int errorCode, int protocolId, byte[] body) {
        this.cid = cid;
        this.errorCode = errorCode;
        this.protocolId = protocolId;
        this.body = body;
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

    public byte[] getBody() {
        return body;
    }
}
