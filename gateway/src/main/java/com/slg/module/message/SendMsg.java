package com.slg.module.message;

import com.google.protobuf.GeneratedMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;


@Component
public class SendMsg {
    public void send(ChannelHandlerContext ctx, byte[] msg) {
        ByteBuf buf = Unpooled.buffer(16);
        //消息头
        buf.writeLong(1234567L);
        buf.writeInt(1);
        buf.writeByte(7);
        buf.writeByte(9);
        int length = msg.length;
        buf.writeShort(length);
        buf.writeBytes(msg);
        Channel channel = ctx.channel();
//        if (channel.isActive()){
//            System.out.println("channel 活跃");
//        }else {
//            System.out.println("channel fasle" +channel);
//        }
        ChannelFuture future = ctx.writeAndFlush(buf);
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                if (future.isSuccess()) {
//                    System.out.println("消息发送成功");
//                } else {
//                    System.out.println("消息发送失败: " + future.cause().getMessage());
//                }
//            }
//        });

    }

    //  com.google.protobuf.GeneratedMessage.Builder<Builder>
    public void send(ChannelHandlerContext ctx, GeneratedMessage.Builder<?> builder) {
        byte[] msg = builder.buildPartial().toByteArray();
        ByteBuf buf = Unpooled.buffer(16);
        //消息头
        buf.writeLong(1234567L);
        buf.writeInt(1);
        buf.writeByte(7);
        buf.writeByte(9);
        int length = msg.length;
        buf.writeShort(length);
        buf.writeBytes(msg);
        ChannelFuture future = ctx.writeAndFlush(buf);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
//                    System.out.println("消息发送成功");
                } else {
//                    System.out.println("消息发送失败: " + future.cause().getMessage());
                }
            }
        });

    }


    /**
     * 客户端消息
     */
    public ByteBuf buildClientMsg(int cid, int errorCode, int protocolId, byte zip, byte encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = Unpooled.buffer(16 + length);
        //消息头
        out.writeInt(cid);      // 4字节
        out.writeInt(errorCode);   // 4字节
        out.writeInt(protocolId);  // 4字节
        out.writeByte(zip);         // zip压缩标志，1字节
        out.writeByte(encrypted);  // 加密标志，1字节
        //消息体
        out.writeShort(length);   // 消息体长度，2字节
        // 写入消息体
        if (body!=null){
            out.writeBytes(body);
        }
        return out;
    }

    /**
     * 服务器信息
     */
    public ByteBuf buildServerMsg(long userId, int cid, int errorCode, int protocolId, int zip, int encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = Unpooled.buffer(24 + length);
        //消息头
        out.writeLong(userId);      // 8字节
        out.writeInt(cid);      // 4字节
        out.writeInt(errorCode);      // 4字节
        out.writeInt(protocolId);      // 4字节
        out.writeByte(zip);                       // zip压缩标志，1字节
        out.writeByte(encrypted);                       // 加密标志，1字节
        //消息体
        out.writeShort(length);                 // 消息体长度，2字节
        // 写入消息体
        if (body!=null){
            out.writeBytes(body);
        }
        return out;
    }

}
