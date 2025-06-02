package com.slg.module.rpc.outside.outsideMsg;


import com.slg.module.message.ByteBufferMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * 注意确保之前的包完整性，入
 */
public class MsgDecode extends ByteToMessageDecoder {
//    @Override
//    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//        // 确保有足够的字节来读取头部
//        if (in.readableBytes() < 16) {
//            return;
//        }
//        // 缓存 readableBytes 不够则暂时到局部变量
//        int readableBytes = in.readableBytes();
//        // 消息头
//        int cid = in.readInt();
//        int errorCode = in.readInt();
//        int protocolId = in.readInt();
//        byte zip = in.readByte();
//        byte pbVersion = in.readByte();
//        short length = in.readShort();
//        // 检查是否有足够的字节来读取整个消息体
//        if (readableBytes < 16 + length) {
//            // 如果没有，丢弃已经读取的头部信息，并返回
//            in.readerIndex(in.readerIndex() - 16);
//            return;
//        }
//        byte[] bytes = new byte[length];
//        in.readBytes(bytes, 0, length);
//        ByteBufferMessage byteBufMessage = new ByteBufferMessage(cid, errorCode, protocolId, bytes);
//        out.add(byteBufMessage);
//    }
//    private static final int MAX_BODY_LENGTH = 10 * 1024 * 1024; // 10MB

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 检查消息头完整性
        if (in.readableBytes() < 16) return;
        // 缓存 readableBytes 不够则暂时到局部变量
        int readableBytes = in.readableBytes();
        // 读取消息头
        int cid = in.readInt();
        int errorCode = in.readInt();
        int protocolId = in.readInt();
        byte zip = in.readByte();
        byte encrypted = in.readByte();
        short length = in.readShort();//边界消息长度，可能不是原始的
        // 检查是否有足够的字节来读取整个消息体
        if (readableBytes < 16 + length) {
            // 如果没有，丢弃已经读取的头部信息，并返回
            in.readerIndex(in.readerIndex() - 16);
            return;
        }
        // 零拷贝切片（引用计数+1）
        ByteBuf body = in.readRetainedSlice(length);
        out.add(ByteBufferMessage.newInstance(cid, errorCode, protocolId, zip, encrypted, length, body));
    }
}
