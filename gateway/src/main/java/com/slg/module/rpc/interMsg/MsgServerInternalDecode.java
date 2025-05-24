package com.slg.module.rpc.interMsg;

import com.slg.module.message.ByteBufferServerMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 注意确保之前的包完整性，入
 */
public class MsgServerInternalDecode extends ByteToMessageDecoder {
//    @Override
//    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//        // 确保有足够的字节来读取头部
//        if (in.readableBytes() < 24) {
//            return;
//        }
//        // 缓存 readableBytes 不够则暂时到局部变量
//        int readableBytes = in.readableBytes();
//
//        // 消息头
//        long userId = in.readLong();
//
//        int cid = in.readInt();
//        int errorCode = in.readInt();
//        int protocolId = in.readInt();
//        byte zip = in.readByte();
//        byte encrypted = in.readByte();
//        short length = in.readShort();
//        // 检查是否有足够的字节来读取整个消息体
//        if (readableBytes < 16 + length) {
//            // 如果没有，丢弃已经读取的头部信息，并返回
//            in.readerIndex(in.readerIndex() - 16);
//            return;
//        }
//
//        byte[] bytes = new byte[length];
//        in.readBytes(bytes, 0, length);
//        ByteBufferServerMessage byteBufMessage = new ByteBufferServerMessage(userId, cid, errorCode, protocolId, zip, encrypted ,bytes);
//        out.add(byteBufMessage);
//
//

    /// /        ByteBuf messageBody = in.readBytes(length);
    /// /        byte[] array = messageBody.array();
    /// /        ByteBuffer byteBuffer = messageBody.nioBuffer();
    /// /        ByteBufferServerMessage byteBufferMessage = new ByteBufferServerMessage(sessionId, cid, errorCode, protocolId, byteBuffer);
    /// /        out.add(byteBufferMessage);
    /// /        //释放 messageBody 的引用
    /// /        messageBody.release();
//    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 确保有足够的字节来读取头部
        if (in.readableBytes() < 24) {
            return;
        }
        // 缓存 readableBytes 不够则暂时到局部变量
        int readableBytes = in.readableBytes();

        // 消息头
        long userId = in.readLong();
        int cid = in.readInt();
        int errorCode = in.readInt();
        int protocolId = in.readInt();
        byte zip = in.readByte();
        byte encrypted = in.readByte();
        short length = in.readShort();
        // 检查是否有足够的字节来读取整个消息体
        if (readableBytes < 24 + length) {
            // 如果没有，丢弃已经读取的头部信息，并返回
            in.readerIndex(in.readerIndex() - 24);
            return;
        }
        // 零拷贝切片（引用计数+1）
        ByteBuf body = in.readRetainedSlice(length);
        out.add(ByteBufferServerMessage.newInstance(userId, cid, errorCode, protocolId, zip, encrypted, length, body));
    }
}
