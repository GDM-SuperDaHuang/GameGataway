package com.slg.module.message;

import com.google.protobuf.GeneratedMessage;

public class MsgResponse {
    private int errorCode;
    private GeneratedMessage.Builder<?> body;

    public MsgResponse() {
    }

    public MsgResponse(int errorCode, GeneratedMessage.Builder<?> body) {
        this.errorCode = errorCode;
        this.body = body;
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
