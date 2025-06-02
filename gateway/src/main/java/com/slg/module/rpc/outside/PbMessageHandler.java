package com.slg.module.rpc.outside;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.slg.module.config.GatewayRoutingManager;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.DHKeyInfo;
import com.slg.module.connection.ServerChannelManage;
import com.slg.module.connection.ServerConfig;
import com.slg.module.message.*;
import com.slg.module.register.HandlePbBeanManager;
import com.slg.module.rpc.interMsg.ForwardClient;
import com.slg.module.util.BeanTool;
import com.slg.module.util.CryptoUtils;
import com.slg.module.util.LZ4Compression;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class PbMessageHandler extends SimpleChannelInboundHandler<ByteBufferMessage> {
    private static final ConcurrentHashMap<Byte, Object> SERVER_LOCKS = new ConcurrentHashMap<>(1);
    // 全局唯一的连接创建锁
    private static final Object CONNECTION_LOCK = new Object();

    private int gateProtoIdMax;
    HandlePbBeanManager handlePbBeanManager = HandlePbBeanManager.getInstance();
    ServerChannelManage serverChannelManage = ServerChannelManage.getInstance();
    ClientChannelManage clientChannelManage = ClientChannelManage.getInstance();
    GatewayRoutingManager gatewayRoutingManager = GatewayRoutingManager.getInstance();

    public PbMessageHandler(int gateProtoIdMax) {
        this.gateProtoIdMax = gateProtoIdMax;
    }


    /**
     * @param clientChannel 客户端-网关连接
     * @param msg           信息
     * @throws Exception 异常
     */

    @Override
    protected void channelRead0(ChannelHandlerContext clientChannel, ByteBufferMessage msg) {
        int protocolId = msg.getProtocolId();
        ByteBuf byteBuf = msg.getBody();
        ChannelId channelId = clientChannel.channel().id();
        Long userId = clientChannelManage.getUserId(channelId);
        ByteBuffer body = byteBuf.nioBuffer();//视图,可能是原始数据
        if (protocolId < gateProtoIdMax) {//本地
            ByteBuf zipBuf = null;//解压缩标志
            ByteBuf decryptBuf = null;//解密标志
            //解密
            if (msg.getEncrypted() == Constants.Encrypted) {
                DHKeyInfo dhKeyInfo = clientChannelManage.getCipher(channelId);
                if (dhKeyInfo == null) {
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                    return;
                }
                try {
                    SecretKey aesKey = CryptoUtils.generateAesKey(dhKeyInfo.getSharedKey());
                    decryptBuf = CryptoUtils.decrypt(aesKey, byteBuf);
                    //得到 原始数据/压缩后的数据
                    body = decryptBuf.nioBuffer();
                } catch (Exception e) {
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                }
            }

            //解压缩
            if (msg.getZip() == Constants.Zip) {
                short originalLength = byteBuf.readShort();
                if (decryptBuf == null) {
                    zipBuf = LZ4Compression.decompress(byteBuf, originalLength);
                    body = zipBuf.nioBuffer();
                } else {
                    zipBuf = LZ4Compression.decompress(decryptBuf, originalLength);
                    body = zipBuf.nioBuffer();
                }
            }

            Method parse = handlePbBeanManager.getParseFromMethod(msg.getProtocolId());
            if (parse == null) {
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                if (zipBuf != null) {
                    zipBuf.release();
                }
                if (decryptBuf != null) {
                    decryptBuf.release();
                }
                return;
            }

            //响应
            MsgResponse response = null;
            try {
                Object msgObject = parse.invoke(null, body);
                response = route(clientChannel, msgObject, protocolId, userId);
            } catch (Exception e) {
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                if (zipBuf != null) {
                    zipBuf.release();
                }
                if (decryptBuf != null) {
                    decryptBuf.release();
                }
                return;
            }

            if (response == null) {
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                if (zipBuf != null) {
                    zipBuf.release();
                }
                if (decryptBuf != null) {
                    decryptBuf.release();
                }
                return;
            }
            if (zipBuf != null) {
                zipBuf.release();
            }
            if (decryptBuf != null) {
                decryptBuf.release();
            }

            //---------------------------------------------------
            GeneratedMessage.Builder<?> responseBody = response.getBody();
            Message message = responseBody.buildPartial();
            //构建响应消息体，失败时，或者没有发送出去都需要手动释放
            ByteBuf respBody = clientChannel.alloc().buffer(message.getSerializedSize());
            //回收 MsgResponse
            response.recycle();
            try {
                message.writeTo(new OutputStream() {
                    @Override
                    public void write(int b) {
                        respBody.writeByte(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        respBody.writeBytes(b, off, len);
                    }
                });
            } catch (IOException e) {
                respBody.release();
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                return;
            }
            //是否需要压缩？ todo
            short bodyLength = (short) respBody.readableBytes(); // 原始数据长度
            //判断
            if (response.isEncrypted() && bodyLength > 20) {// 加密+压缩
                //获取密钥
                SecretKey aesKey;
                DHKeyInfo dhKeyInfo = clientChannelManage.getCipher(channelId);
                try {
                    aesKey = CryptoUtils.generateAesKey(dhKeyInfo.getSharedKey());
                } catch (Exception e) {
                    respBody.release();
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                    return;
                }

                //先压缩，后加密
                ByteBuf compressBuf = LZ4Compression.compressWithLengthHeader(respBody, bodyLength);
                short encryptedLength;
                ByteBuf encryptedBuf;
                try {
                    //加密
                    encryptedBuf = CryptoUtils.encrypt(aesKey, compressBuf);
                    //加密后的长度
                    encryptedLength = (short) compressBuf.readableBytes();
                } catch (Exception e) {
                    respBody.release();
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                    return;
                }
                //释放
                compressBuf.release();
                respBody.release();
                ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.Zip, Constants.Encrypted, encryptedLength, encryptedBuf);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
                channelFuture.addListener(future -> {
                    msg.recycle();
                    if (!future.isSuccess()) {
                        out.release();
                        System.err.println("Write and flush failed: " + future.cause());
                    }
                });
            } else if (response.isEncrypted()) {// 仅加密
                //获取密钥
                SecretKey aesKey;
                DHKeyInfo dhKeyInfo = clientChannelManage.getCipher(channelId);
                try {
                    aesKey = CryptoUtils.generateAesKey(dhKeyInfo.getSharedKey());
                } catch (Exception e) {
                    respBody.release();
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                    return;
                }
                short encryptedLength;
                ByteBuf encryptedBuf;
                try {
                    encryptedBuf = CryptoUtils.encrypt(aesKey, respBody);
                    //加密后的长度
                    encryptedLength = (short) encryptedBuf.readableBytes();
                } catch (Exception e) {
                    respBody.release();
                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                    return;
                }
                //释放
                respBody.release();
                ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.Encrypted, encryptedLength, encryptedBuf);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
                channelFuture.addListener(future -> {
                    msg.recycle();
                    if (!future.isSuccess()) {
                        out.release();
                        System.err.println("Write and flush failed: " + future.cause());
                    }
                });
            } else if (bodyLength > 20) {// 仅压缩
                //先压缩，后加密
                ByteBuf compressBuf = LZ4Compression.compressWithLengthHeader(respBody, bodyLength);
                short zipLength = (short) compressBuf.readableBytes();
                if (zipLength < bodyLength) {
                    ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.Zip, Constants.NoEncrypted, zipLength, compressBuf);
                    ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
                    channelFuture.addListener(future -> {
                        msg.recycle();
                        if (!future.isSuccess()) {
                            out.release();
                            System.err.println("Write and flush failed: " + future.cause());
                        }
                    });
                } else {
                    compressBuf.release();
                    ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
                    ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
                    channelFuture.addListener(future -> {
                        msg.recycle();
                        if (!future.isSuccess()) {
                            out.release();
                            System.err.println("Write and flush failed: " + future.cause());
                        }
                    });
                }

            } else {// 什么也不干
                ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
                channelFuture.addListener(future -> {
                    msg.recycle();
                    if (!future.isSuccess()) {
                        out.release();
                        System.err.println("Write and flush failed: " + future.cause());
                    }
                });
            }


//            //是否需要加密
//            if (aesKey != null && compressBuf == null) {//仅加密
//                try {
//                    encryptedBuf = CryptoUtils.encrypt(aesKey, respBody);
//                } catch (Exception e) {
//                    respBody.release();
//                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
//                    return;
//                }
//                //释放
//                respBody.release();
//                out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.Encrypted, bodyLength, encryptedBuf);
//                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
//                channelFuture.addListener(future -> {
//                    msg.recycle();
//                    if (!future.isSuccess()) {
//                        out.release();
//                        System.err.println("Write and flush failed: " + future.cause());
//                    }
//                });
//
//            } else if (aesKey != null && compressBuf != null) {//加密+压缩
//                try {
//                    encryptedBuf = CryptoUtils.encrypt(aesKey, compressBuf);
//                } catch (Exception e) {
//                    respBody.release();
//                    failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
//                    return;
//                }
//                //释放
//                compressBuf.release();
//                respBody.release();
//                out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.Zip, Constants.Encrypted, bodyLength, encryptedBuf);
//                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
//                channelFuture.addListener(future -> {
//                    msg.recycle();
//                    if (!future.isSuccess()) {
//                        out.release();
//                        System.err.println("Write and flush failed: " + future.cause());
//                    }
//                });
//                return;
//            } else if (aesKey == null && compressBuf != null) {//仅压缩
//                out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, compressBuf);
//                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
//                channelFuture.addListener(future -> {
//                    msg.recycle();
//                    if (!future.isSuccess()) {
//                        out.release();
//                        System.err.println("Write and flush failed: " + future.cause());
//                    }
//                });
//
//            } else {//不加密+不压缩
//                out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
//                ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
//                channelFuture.addListener(future -> {
//                    msg.recycle();
//                    if (!future.isSuccess()) {
//                        out.release();
//                        System.err.println("Write and flush failed: " + future.cause());
//                    }
//                });
//
//            }
//            processingResponse(msgResponse, clientChannel, msg, userId);
//            TestMsg.getInstance(protocolId).printStats();


        } else if (userId != null) {//转发
            // todo gatewayRoutingManager使用注册中心代替
            ServerConfig serverConfig = gatewayRoutingManager.getServer(protocolId, userId, 0);
            if (serverConfig == null) {
                // 转发失败,直接返回，告诉客户端
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.ESTABLISH_CONNECTION_FAILED);
                return;
            }
            forwardToTargetServer(clientChannel, msg, userId, serverConfig);
        } else {
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.NOT_LOGGED_IN);
        }
    }

//    @Override
//    protected void channelRead0(ChannelHandlerContext clientChannel, ByteBufferMessage msg) throws Exception {
//        int protocolId = msg.getProtocolId();
//        ByteBuf byteBuf = msg.getBody();
//        ChannelId channelId = clientChannel.channel().id();
//        Long userId = clientChannelManage.getUserId(channelId);
//        ByteBuffer body = byteBuf.nioBuffer();//视图,可能是原始数据
//        if (protocolId < gateProtoIdMax) {//本地
//            Method parse = handlePbBeanManager.getParseFromMethod(msg.getProtocolId());
//            Object msgObject = parse.invoke(null, body);
//            MsgResponse response = route(clientChannel, msgObject, protocolId, userId);
//            //写回
//            GeneratedMessage.Builder<?> responseBody = response.getBody();
//            Message message = responseBody.buildPartial();
//            //构建响应消息体，失败时，或者没有发送出去都需要手动释放
//            ByteBuf respBody = clientChannel.alloc().buffer(message.getSerializedSize());
//            //回收 MsgResponse
//            response.recycle();
//            message.writeTo(new OutputStream() {
//                @Override
//                public void write(int b) {
//                    respBody.writeByte(b);
//                }
//
//                @Override
//                public void write(byte[] b, int off, int len) {
//                    respBody.writeBytes(b, off, len);
//                }
//            });
//            short bodyLength = (short) respBody.readableBytes(); // 这里获取长度
//            ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
//            ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
//            channelFuture.addListener(future -> {
//                msg.recycle();
//                if (!future.isSuccess()) {
//                    out.release();
//                    System.err.println("Write and flush failed: " + future.cause());
//                }
//            });
////            TestMsg.getInstance(protocolId).printStats();
//        } else if (userId != null) {//转发
//            // todo gatewayRoutingManager使用注册中心代替
//            ServerConfig serverConfig = gatewayRoutingManager.getServer(protocolId, userId, 0);
//            if (serverConfig == null) {
//                // 转发失败,直接返回，告诉客户端
//                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.ESTABLISH_CONNECTION_FAILED);
//                return;
//            }
//            forwardToTargetServer(clientChannel, msg, userId, serverConfig);
//        } else {
//            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.NOT_LOGGED_IN);
//        }
//    }

    /**
     * 发送时:先压缩后加密原则
     * 处理请求，管理zip，decryptBuf，byteBuf的释放
     *
     * @param clientChannel
     * @param msg
     * @param body          视图,可能是原始数据
     * @param userId
     * @throws Exception
     */
    //处理请求
    private MsgResponse processingRequests(ChannelHandlerContext clientChannel, ByteBufferMessage msg, ByteBuffer body, Long userId) {
        ByteBuf zipBuf = null;
        ByteBuf decryptBuf = null;
        ByteBuf byteBuf = msg.getBody();
        int protocolId = msg.getProtocolId();
        //解密
        if (msg.getEncrypted() == Constants.Encrypted) {
            DHKeyInfo dhKeyInfo = clientChannelManage.getCipher(userId);
            if (dhKeyInfo == null) {
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                return null;
            }
            try {
                SecretKey aesKey = CryptoUtils.generateAesKey(dhKeyInfo.getSharedKey());
                decryptBuf = CryptoUtils.decrypt(aesKey, byteBuf);
            } catch (Exception e) {
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
            }
        }


        //解压缩
        if (msg.getZip() == Constants.Zip && decryptBuf == null) {
            zipBuf = LZ4Compression.decompress(byteBuf, msg.getLength());
            body = zipBuf.nioBuffer();
        } else if (msg.getZip() == Constants.Zip && decryptBuf != null) {
            zipBuf = LZ4Compression.decompress(decryptBuf, msg.getLength());
            body = zipBuf.nioBuffer();
        }
        Method parse = handlePbBeanManager.getParseFromMethod(msg.getProtocolId());
        if (parse == null) {
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
            if (zipBuf != null) {
                zipBuf.release();
            }
            if (decryptBuf != null) {
                decryptBuf.release();
            }
            return null;
        }

        //响应
        MsgResponse response = null;
        try {
            Object msgObject = parse.invoke(null, body);
            response = route(clientChannel, msgObject, protocolId, userId);
        } catch (Exception e) {
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
            if (zipBuf != null) {
                zipBuf.release();
            }
            if (decryptBuf != null) {
                decryptBuf.release();
            }
            return null;
        }

        if (response == null) {
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
            if (zipBuf != null) {
                zipBuf.release();
            }
            if (decryptBuf != null) {
                decryptBuf.release();
            }
            return null;
        }
        if (zipBuf != null) {
            zipBuf.release();
        }
        if (decryptBuf != null) {
            decryptBuf.release();
        }
        return response;
    }

    private MsgResponse processingRequests1(ChannelHandlerContext clientChannel, ByteBufferMessage msg, ByteBuffer body, Long userId) throws Exception {
        int protocolId = msg.getProtocolId();
        Method parse = handlePbBeanManager.getParseFromMethod(msg.getProtocolId());
        Object msgObject = parse.invoke(null, body);
        MsgResponse response = route(clientChannel, msgObject, protocolId, userId);
        return response;
    }

    /**
     * 回发消息，处理
     * <p>
     * 管理 msg ,respBody，compressBuf,encryptedBuf
     *
     * @param response
     * @param clientChannel
     * @param msg
     * @param userId
     * @throws Exception
     */
    private void processingResponse(MsgResponse response, ChannelHandlerContext clientChannel, ByteBufferMessage msg, long userId) {
        //写回
        GeneratedMessage.Builder<?> responseBody = response.getBody();
        Message message = responseBody.buildPartial();
        //构建响应消息体，失败时，或者没有发送出去都需要手动释放
        ByteBuf respBody = clientChannel.alloc().buffer(message.getSerializedSize());
        int protocolId = msg.getProtocolId();
        //回收 MsgResponse
        response.recycle();
        try {
            message.writeTo(new OutputStream() {
                @Override
                public void write(int b) {
                    respBody.writeByte(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    respBody.writeBytes(b, off, len);
                }
            });
        } catch (IOException e) {
            respBody.release();
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
            return;
        }

        short bodyLength = (short) respBody.readableBytes(); // 这里获取长度
        //加密，压缩判断
        ByteBuf compressBuf = null;
        ByteBuf encryptedBuf = null;
        final ByteBuf out;
        SecretKey aesKey = null;
        DHKeyInfo dhKeyInfo = null;

        //是否需要加密
        if (response.isEncrypted()) {
            dhKeyInfo = clientChannelManage.getCipher(userId);
            try {
                aesKey = CryptoUtils.generateAesKey(dhKeyInfo.getSharedKey());
            } catch (Exception e) {
                respBody.release();
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                return;
            }
        }

        //压缩判断 todo
        if (bodyLength > 1024) {
            compressBuf = LZ4Compression.compress(respBody);
        }

        //是否需要加密
        if (aesKey != null && compressBuf == null) {//加密但是不压缩
            try {
                encryptedBuf = CryptoUtils.encrypt(aesKey, respBody);
            } catch (Exception e) {
                respBody.release();
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                return;
            }
            //释放
            respBody.release();
            out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.Encrypted, bodyLength, encryptedBuf);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    out.release();
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });

        } else if (aesKey != null && compressBuf != null) {//加密+压缩
            try {
                encryptedBuf = CryptoUtils.encrypt(aesKey, compressBuf);
            } catch (Exception e) {
                respBody.release();
                failedNotificationClient(clientChannel, msg, ErrorCodeConstants.SERIALIZATION_METHOD_LACK);
                return;
            }
            //释放
            compressBuf.release();
            respBody.release();
            out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.Zip, Constants.Encrypted, bodyLength, encryptedBuf);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    out.release();
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });
            return;
        } else if (aesKey == null && compressBuf != null) {//仅压缩
            out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, compressBuf);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
            channelFuture.addListener(future -> {
                msg.recycle();
                if (!future.isSuccess()) {
                    out.release();
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });

        } else {//不加密+不压缩
            out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
            channelFuture.addListener(future -> {
                msg.recycle();
                if (!future.isSuccess()) {
                    out.release();
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });

        }
    }

    private void processingResponse1(MsgResponse response, ChannelHandlerContext clientChannel, ByteBufferMessage msg, long userId) throws IOException {
        //写回
        GeneratedMessage.Builder<?> responseBody = response.getBody();
        Message message = responseBody.buildPartial();
        //构建响应消息体，失败时，或者没有发送出去都需要手动释放
        ByteBuf respBody = clientChannel.alloc().buffer(message.getSerializedSize());
        int protocolId = msg.getProtocolId();
        //回收 MsgResponse
        response.recycle();
        message.writeTo(new OutputStream() {
            @Override
            public void write(int b) {
                respBody.writeByte(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                respBody.writeBytes(b, off, len);
            }
        });
        short bodyLength = (short) respBody.readableBytes(); // 这里获取长度
        ByteBuf out = MsgUtil.buildClientMsg(clientChannel, msg.getCid(), response.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyLength, respBody);
        ChannelFuture channelFuture = clientChannel.writeAndFlush(out);
        channelFuture.addListener(future -> {
            msg.recycle();
            if (!future.isSuccess()) {
                out.release();
                System.err.println("Write and flush failed: " + future.cause());
            }
        });


    }

    /**
     * 转发到目标服务器
     *
     * @param clientChannel 客户端-网关
     * @param msg           消息
     */
    private void forwardToTargetServer(ChannelHandlerContext clientChannel, ByteBufferMessage msg, long userId, ServerConfig serverConfig) {
        Channel channel = serverChannelManage.getChanel(serverConfig.getServerId());
        if (channel == null) {
            // 获取当前 serverId 对应的专用锁对象
            Object lock = SERVER_LOCKS.computeIfAbsent(serverConfig.getServerId(), k -> new Object());
            synchronized (lock) {
                channel = ForwardClient.getInstance().connection(serverConfig);
            }
        }
        //进行转发到目标服务器
        if (channel != null && channel.isActive()) {
            forward(channel, clientChannel, msg, userId, ErrorCodeConstants.GATE_FORWARDING_FAILED, serverConfig);
        } else {
            failedNotificationClient(clientChannel, msg, ErrorCodeConstants.GATE_FORWARDING_FAILED);
        }
    }


    //客户端端处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof InvocationTargetException) {
            //目标方法错误
        } else if (cause instanceof SocketException
                || cause instanceof DecoderException) {
            //客户端关闭连接/连接错误
            // 关闭连接
            //断开无效连接
            destroyConnection(ctx);
            ctx.close();
        }
    }


    /**
     * 路由分发
     */
    public MsgResponse route(ChannelHandlerContext ctx, Object message, int protocolId, Long userId) throws Exception {
        Class<?> handleClazz = handlePbBeanManager.getClassHandle(protocolId);
        if (handleClazz == null) {
            return null;
        }
        Method method = handlePbBeanManager.getHandleMethod(protocolId);
        if (method == null) {
            return null;
        }
        method.setAccessible(true);
        Object bean = BeanTool.getBean(handleClazz);
        if (bean == null) {
            return null;
        }
        Object invoke = method.invoke(bean, ctx, message, userId);
        if (invoke instanceof MsgResponse) {
            return (MsgResponse) invoke;
        } else {
            return null;
        }
    }


    public void destroyConnection(ChannelHandlerContext ctx) {
        //断开无效连接
        clientChannelManage.remove(ctx.channel());
    }

    // 失败通知客户端
    public void failedNotificationClient(ChannelHandlerContext ctx, ByteBufferMessage msg, int errorCode) {
        //日志记录失败日志 todo
        // 发送失败,直接返回，告诉客户端
        ByteBuf out = MsgUtil.buildClientMsg(ctx, msg.getCid(), errorCode, msg.getProtocolId(), Constants.NoZip, Constants.NoEncrypted, Constants.NoLength, null);
        ChannelFuture channelFuture = ctx.writeAndFlush(out);
        channelFuture.addListener(future -> {
            msg.recycle();
        });

    }

    //发送消息
    public void forward(Channel serverChannel, ChannelHandlerContext clientChannel, ByteBufferMessage msg, long userId, int errorCode, ServerConfig serverConfig) {
        ByteBuf out = MsgUtil.buildServerMsg(clientChannel, userId, msg.getCid(), msg.getErrorCode(), msg.getProtocolId(), msg.getZip(), msg.getEncrypted(), msg.getLength(), msg.getBody());
        ChannelFuture channelFuture = serverChannel.writeAndFlush(out);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                //释放
                msg.recycle();
            } else {
                // 消息转发失败的处理
                // log.error("Failed to forward message to {}", targetServerAddress, future.cause());
                serverChannelManage.removeServerChanel(serverConfig.getServerId());
                //直接告诉客户端，返回错误码
                failedNotificationClient(clientChannel, msg, errorCode);
            }
        });
    }


}
